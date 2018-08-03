import com.icthh.xm.commons.exceptions.BusinessException
import com.icthh.xm.commons.logging.util.MdcUtils
import com.icthh.xm.ms.entity.domain.ext.IdOrKey
import org.apache.commons.lang3.time.DateFormatUtils
import org.slf4j.LoggerFactory
import org.springframework.data.jpa.domain.Specifications

import javax.persistence.criteria.JoinType

import static groovy.json.JsonOutput.toJson

import static groovyx.net.http.Method.POST
import static groovyx.net.http.Method.GET
import static groovyx.net.http.ContentType.JSON
import static groovyx.net.http.ContentType.URLENC
import groovyx.net.http.HTTPBuilder

log = LoggerFactory.getLogger(getClass())

//--------------------- MAIN ------------------------------------------------
idOrKey = lepContext.inArgs.idOrKey
entityService = lepContext.services.xmEntity
linkService = lepContext.services.linkService

// get entity without lep
entity = entityService.findAll(Specifications.where({ root, query, cb ->
    return idOrKey.isId() ? cb.and(cb.equal(root.get("id"), idOrKey.id)) : cb.and(cb.equal(root.get("key"), idOrKey.key))
})).first()
// TODO comment/remove it  because this is not actual for current xm-ms-entity version
// clear targets to prevent problem with entity persisting
entity.targets.clear()

typeKey = entity.typeKey
from = entity.stateKey
to = lepContext.inArgs.nextStateKey

log.info("Change state for entity {}:{}={} {} -> {}", typeKey, idOrKey.isId() ? "id" : "key", idOrKey, from, to)

transition = [
        "PRODUCT.AEPRODUCT.CAR-CHARGE.AECHARGE":
                [
                        "ACTIVE"          : ["FINISH_REQUESTED": { finishChargeRequested() }],
                        "FINISH_REQUESTED": ["FINISH_REQUESTED": { finishChargeRequested() }]
                ],
        "PRODUCT.AEPRODUCT.CAR-RENT"           :
                [
                        "ACTIVE"         : ["CANCELED"       : { rentCanceled(from, to) },
                                            "CHECKUP_STARTED": { updateAeProductState(from, to) }],
                        "CHECKUP_STARTED": ["RENT_STARTED": { startRent() },
                                            "CANCELED"    : { rentCanceled(from, to) }],
                        "RENT_STARTED"   : ["RENT_FINISHED"            : { rentFinished(from, to) },
                                            "RENT_FINISHED_OCCUPIED"   : { rentFinishedOccupied(from, to) },
                                            "RENT_FINISHED_OTHER_PLACE": { rentFinishedOtherPlace(from, to) },
                                            "RENT_PAUSED"              : { rentPaused() }],
                        "RENT_PAUSED"    : ["RENT_STARTED"             : { updateAeProductState(from, to) },
                                            "RENT_FINISHED"            : { rentFinished(from, to) },
                                            "RENT_FINISHED_OCCUPIED"   : { rentFinishedOccupied(from, to) },
                                            "RENT_FINISHED_OTHER_PLACE": { rentFinishedOtherPlace(from, to) }]
                ],
        "PRODUCT.SERVICE.CAR-SHARING"          : [
                "NEW"     : ["VERIFIED": { verifyCarSharingService() }],
                "VERIFIED": ["ACTIVE": { activateCarSharingService() }]
        ]
]

transitionFunc = findTransition(typeKey, from, to)

if (transitionFunc) {
    log.info("Execute transition func for [{}] from [{}] to [{}]", typeKey, from, to)
    transitionFunc.call()
}

log.info("Update state to [$to] for xm-entity: $entity")

entity.setStateKey(to)

return entityService.save(entity)

def findTransition(typeKey, from, to) {
    def translatedTypeKey = typeKey

    // PRODUCT.AEPRODUCT.CAR-RENT.*
    if (typeKey.startsWith('PRODUCT.AEPRODUCT.CAR-RENT')) translatedTypeKey = 'PRODUCT.AEPRODUCT.CAR-RENT'
    //PRODUCT.SERVICE.CAR-SHARING.*
    if (typeKey.startsWith('PRODUCT.SERVICE.CAR-SHARING')) translatedTypeKey = 'PRODUCT.SERVICE.CAR-SHARING'

    return transition.get(translatedTypeKey)?.get(from)?.get(to)
}

//--------------------- PRODUCT.AEPRODUCT.CAR-CHARGE.AECHARGE ---------------------
def finishChargeRequested() {
    def pathToEcsApi = lepContext.services.tenantConfigService.config?.integration?.aeApiUrl
    def accessToken = getAccessToken()
    def creationDate = DateFormatUtils.ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.format(new Date())

    def request = [
            source        : 'XM-BE',
            eventType     : 'PRODUCT_STATE_CHANGED',
            messagingType : 'ASYNC',
            eventUuid     : MdcUtils.getRid(),
            eventTimestamp: creationDate,
            entityTypeKey : 'PRODUCT.AEPRODUCT.CAR-CHARGE.AECHARGE',
            xmProductId   : entity.id,
            aeProductId   : entity.data.transactionId,
            eventData     : [
                    xmFromState: from,
                    xmToState  : to
            ]
    ]
    log.info('POST request to /updateProduct BODY {}', toJson(request))

    http = new HTTPBuilder(pathToEcsApi + '/updateProduct?access_token=' + accessToken)
    http.ignoreSSLIssues()

    http.request(POST, JSON) {
        body = request
        response.failure = {
            resp, json ->
                log.info('Response from /updateProduct BODY {}', resp.statusLine)
                if (resp.statusLine.statusCode == 400 && json.toString().contains('Error in stopping transaction')) {
                    return
                } else {
                    throw new BusinessException('error.external.ae.api', "${resp.statusLine} BODY ${json}")
                }
        }

        response.success = {
            resp, json ->
                log.info('Response from /updateProduct BODY {}', json)
        }
    }
}

//--------------------- PRODUCT.AEPRODUCT.CAR-RENT ---------------------
def getCarInRent(rentProduct) {
    foundCars = linkService.findAll(Specifications.where({ root, query, cb ->
        source = root.join("source", JoinType.INNER)
        target = root.join("target", JoinType.INNER)

        return cb.and(
                cb.equal(source.get("id"), rentProduct.id),
                cb.equal(target.get("typeKey"), "RESOURCE.CAR.AE")
        )
    })).collect({ it -> it.target })

    if (!foundCars) throw new IllegalStateException("Car not found for rent ${rentProduct.id}")
    if (foundCars.size() > 1) throw new IllegalStateException("More then one car (" +
            foundCars.collect({ it -> it.id }) + ") found for rent ${rentProduct.id}")

    return foundCars.first()
}

def getRentUser(rentProduct) {
    foundUsers = linkService.findAll(Specifications.where({ root, query, cb ->
        source = root.join("source", JoinType.INNER)
        target = root.join("target", JoinType.INNER)

        return cb.and(
                cb.equal(target.get("id"), rentProduct.id),
                cb.equal(source.get("typeKey"), "ACCOUNT.USER")
        )
    })).collect({ it -> it.source })

    if (!foundUsers) throw new IllegalStateException("User not found for rent ${rentProduct.id}")
    if (foundUsers.size() > 1) throw new IllegalStateException("More then one user (" +
            foundUsers.collect({ it -> it.id }) + ") found for rent ${rentProduct.id}")

    return foundUsers.first()
}

def startRent() {
    carInRent = getCarInRent(entity)
    if ("RESERVED" != carInRent.stateKey)
        throw new BusinessException("Car {} in wrong state. Found ${carInRent.stateKey} expected RESERVED")

    entityService.updateState(IdOrKey.of(carInRent.id), 'IN_USE', [:])

    entity.data.startRentDate = new Date().getTime().toString()

    updateAeProductState('CHECKUP_STARTED', 'RENT_STARTED')
}

def rentFinished(from, to) {
    carTelemetry = commonFinishRent()

    if (!carTelemetry) throw new BusinessException("ECS-0010", "Unable to connect to vehicle")
    if (!carTelemetry?.isCharging) throw new BusinessException("ECS-0011", "Vehicle does not charging")
    if (!carTelemetry?.isDoorsClosed) throw new BusinessException("ECS-0012", "Doors aren't closed")
    if (carTelemetry?.isMoving) throw new BusinessException("ECS-0013", "Vehicle is moving")

    updateAeProductState(from, to)
}

def rentFinishedOccupied(from, to) {
    carTelemetry = commonFinishRent()

    if (!carTelemetry) throw new BusinessException("ECS-0010", "Unable to connect to vehicle")
    if (!carTelemetry?.isDoorsClosed) throw new BusinessException("ECS-0012", "Doors aren't closed")
    if (carTelemetry?.isMoving) throw new BusinessException("ECS-0013", "Vehicle is moving")

    updateAeProductState(from, to)
}

def rentFinishedOtherPlace() {
    carTelemetry = commonFinishRent()

    if (!carTelemetry) throw new BusinessException("ECS-0010", "Unable to connect to vehicle")
    if (!carTelemetry?.isDoorsClosed) throw new BusinessException("ECS-0012", "Doors aren't closed")
    if (carTelemetry?.isMoving) throw new BusinessException("ECS-0013", "Vehicle is moving")

    updateAeProductState(from, to)
}

def commonFinishRent() {
    def carInRent = getCarInRent(entity)
    def rentUser = getRentUser(entity)

    //make car available
    entityService.updateState(IdOrKey.of(carInRent.id), 'AVAILABLE', [:])

    //unlink car and account
    accountCarsLinks = linkService.findAll(Specifications.where({ root, query, cb ->
        source = root.join("source", JoinType.INNER)
        target = root.join("target", JoinType.INNER)

        return cb.and(
                cb.equal(source.get("id"), carInRent.id),
                cb.equal(target.get("id"), rentUser.id)
        )
    }))

    if (!accountCarsLinks) throw new BusinessException("Link between car ${carInRent.id} and user ${rentUser.id} not found")
    accountCarsLinks.each {
        log.info("Remove link between account {} and car {}", rentUser.id, carInRent.id)
        linkService.delete(it.id)
    }

    def pathToAeApi = lepContext.services.tenantConfigService.config?.integration?.aeApiUrl
    def vin = carInRent.data.vin
    def requestBody = [vin: vin]

    def defaultStatusCheck = {
        resp, json ->
            if (true != json?.status) throw new BusinessException("Response status is not valid. ${json}")
            return json
    }

    //disable driving
    doHttpRequest(pathToAeApi + '/disableDriving', POST,
            defaultStatusCheck,
            { resp, json -> throw new BusinessException('error.external.ae.api', "${json}") },
            requestBody,
            URLENC
    )

    //lock car
    doHttpRequest(pathToAeApi + '/lockCar', POST,
            defaultStatusCheck,
            { resp, json -> throw new BusinessException('error.external.ae.api', "${json}") },
            requestBody,
            URLENC
    )

    //set rent finish date
    entity.data.endRentDate = new Date().getTime().toString()

    //return telemetry for check
    return doHttpRequest(pathToAeApi + "/getCarTelemetry/${vin}", GET,
            { resp, json -> return json },
            { resp, json -> throw new BusinessException('error.external.ae.api', "${json}") },
            null,
            null
    )
}

def rentPaused() {
    def carInRent = getCarInRent(entity)
    def pathToAeApi = lepContext.services.tenantConfigService.config?.integration?.aeApiUrl
    def vin = carInRent.data.vin
    def requestBody = [vin: vin]

    def defaultStatusCheck = {
        resp, json ->
            if (true != json?.status) throw new BusinessException("Response status is not valid. ${json}")
            return json
    }

    //lock car
    doHttpRequest(pathToAeApi + '/lockCar', POST,
            defaultStatusCheck,
            { resp, json -> throw new BusinessException('error.external.ae.api', "${json}") },
            requestBody,
            URLENC
    )

    //telemetry for check
    carTelemetry = doHttpRequest(pathToAeApi + "/getCarTelemetry/${vin}", GET,
            { resp, json -> return json },
            { resp, json -> throw new BusinessException('error.external.ae.api', "${json}") },
            null,
            null
    )

    if (!carTelemetry) throw new BusinessException("ECS-0010", "Unable to connect to vehicle")
    if (!carTelemetry?.isCharging) throw new BusinessException("ECS-0011", "Vehicle does not charging")
    if (!carTelemetry?.isDoorsClosed) throw new BusinessException("ECS-0012", "Doors aren't closed")
    if (carTelemetry?.isMoving) throw new BusinessException("ECS-0013", "Vehicle is moving")

    updateAeProductState('RENT_STARTED', 'RENT_PAUSED')
}

def rentCanceled(from, to) {
    def carInRent = getCarInRent(entity)
    def rentUser = getRentUser(entity)

    //make car available
    entityService.updateState(IdOrKey.of(carInRent.id), 'AVAILABLE', [:])

    //unlink car and account
    accountCarsLinks = linkService.findAll(Specifications.where({ root, query, cb ->
        source = root.join("source", JoinType.INNER)
        target = root.join("target", JoinType.INNER)

        return cb.and(
                cb.equal(source.get("id"), carInRent.id),
                cb.equal(target.get("id"), rentUser.id)
        )
    }))

    if (!accountCarsLinks) throw new BusinessException("Link between car ${carInRent.id} and user ${rentUser.id} not found")
    accountCarsLinks.each {
        log.info("Remove link between account {} and car {}", rentUser.id, carInRent.id)
        linkService.delete(it.id)
    }

    def pathToAeApi = lepContext.services.tenantConfigService.config?.integration?.aeApiUrl
    def vin = carInRent.data.vin
    def requestBody = [vin: vin]

    def defaultStatusCheck = {
        resp, json ->
            if (true != json?.status) throw new BusinessException("Response status is not valid. ${json}")
            return json
    }

    //lock car
    doHttpRequest(pathToAeApi + '/lockCar', POST,
            defaultStatusCheck,
            { resp, json -> throw new BusinessException('error.external.ae.api', "${json}") },
            requestBody,
            URLENC
    )

    //telemetry for check
    carTelemetry = doHttpRequest(pathToAeApi + "/getCarTelemetry/${vin}", GET,
            { resp, json -> return json },
            { resp, json -> throw new BusinessException('error.external.ae.api', "${json}") },
            null,
            null
    )

    if (!carTelemetry) throw new BusinessException("ECS-0010", "Unable to connect to vehicle")
    if (!carTelemetry?.isDoorsClosed) throw new BusinessException("ECS-0012", "Doors aren't closed")
    if (carTelemetry?.isMoving) throw new BusinessException("ECS-0013", "Vehicle is moving")

    updateAeProductState(from, to)
}

//--------------------- PRODUCT.SERVICE.CAR-SHARING -------------------------------
def verifyCarSharingService() {
    accountLinks = entityService.getLinkTargets(idOrKey, "ACCOUNT.USER")
    if (!accountLinks) throw new BusinessException("User not found for car sharing service with id " + idOrKey)
    account = accountLinks.first().target

    //Send SMS about verification
    sendAeSMS(account.data.phone,
            'Заявка на услугу EcoCarsharing принята. Ждем Вас в одном из офисов для подписания договора. ' +
                    'Подробная информация по тел. (097) 535 7777',
            'AEnterprise')
}

def activateCarSharingService() {
    accountLinks = entityService.getLinkTargets(idOrKey, "ACCOUNT.USER")
    if (!accountLinks) throw new BusinessException("User not found for car sharing service with id " + idOrKey)
    account = accountLinks.first().target

    //Send SMS service activation
    sendAeSMS(account.data.phone,
            'Услуга EcoCarsharing активирована. Чтобы начать аренду, выберите доступный автомобиль. ' +
                    'Подробная информация по тел. (097) 535 7777',
            'AEnterprise')
}

//-------------------------------- Helpers ----------------------------------------
def getAccessToken() {
    return lepContext.authContext.getAdditionalDetailsValue('aeAccessToken').orElseThrow({
        throw new BusinessException('error.external.ae.api', 'AE access token not found for this account')
    })
}

def getCurrentUserEntity() {
    return lepContext.services.profileService.getSelfProfile().xmentity
}

def updateAeProductState(from, to) {
//    def request = [
//            source        : 'XM-BE',
//            eventType     : 'PRODUCT_STATE_CHANGED',
//            messagingType : 'ASYNC',
//            eventUuid     : MdcUtils.getRid(),
//            eventTimestamp: DateFormatUtils.ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.format(new Date()),
//            entityTypeKey : 'PRODUCT.AEPRODUCT.CAR-RENT.PER-MIN-RENT',
//            xmProductId   : entity.id.toString(),
//            aeProductId   : Long.valueOf(entity.data.aeProductId),
//            eventData     : [
//                    xmFromState: from,
//                    xmToState  : to,
//                    xmAccountId: getCurrentUserEntity().id
//            ]
//    ]
//    log.info('POST request to /updateProduct BODY {}', toJson(request))
//
//    def pathToAeApi = lepContext.services.tenantConfigService.config?.integration?.aeApiUrl
//
//    http = new HTTPBuilder(pathToAeApi + '/updateProduct?access_token=' + getAccessToken())
//    http.ignoreSSLIssues()
//
//    http.request(POST, JSON) {
//        body = request
//        response.failure = {
//            resp, json ->
//                throw new BusinessException('error.external.ae.api', "${json}")
//        }
//
//        response.success = {
//            resp, json ->
//                log.info('Response from /updateProduct BODY {}', json)
//                if (json.eventUuid.isEmpty()) {
//                    throw new BusinessException('error.external.ae.api', "${json}")
//                }
//        }
//    }
}

def doHttpRequest(url, requestType, successFunc, failureFunc, reqBody, contentType) {
    def http = new HTTPBuilder(url)
    http.ignoreSSLIssues()

    http.request(requestType) {
        if (contentType) {
            requestContentType = contentType
        }
        if (reqBody) {
            body = reqBody
        }
        response.failure = failureFunc
        response.success = successFunc
    }
}

def sendAeSMS(msisdn, message, sender) {
    def pathToAeApi = lepContext.services.tenantConfigService.config?.integration?.aeApiUrl
    url = pathToAeApi + '/sendSMS'
    http = new HTTPBuilder(url)
    http.ignoreSSLIssues()

    sendSMSBody = [msisdn: msisdn, message: message, sender: sender]
    log.info('POST request to /sendSMS BODY {}', sendSMSBody)
    http.request(POST, URLENC) {
        body = sendSMSBody
        response.failure = {
            resp ->
                log.info('Response from /sendSMS STATUS {}', resp.statusLine)
                throw new BusinessException('error.external.ae.api', "${resp.statusLine}")
        }

        response.success = {
            resp ->
                log.info('Response from /sendSMS BODY {}', resp.statusLine)
        }
    }
}
