import com.icthh.xm.commons.exceptions.BusinessException

import com.icthh.xm.commons.logging.util.MdcUtils
import com.icthh.xm.ms.entity.config.Constants
import com.icthh.xm.ms.entity.domain.XmEntity
import com.icthh.xm.ms.entity.domain.Link
import com.icthh.xm.ms.entity.domain.ext.IdOrKey
import com.icthh.xm.ms.entity.domain.ext.XmEntityKey
import com.icthh.xm.ms.entity.service.XmEntityService
import com.icthh.xm.ms.entity.util.XmEntityUtils
import groovyx.net.http.HTTPBuilder
import groovyx.net.http.Method
import org.slf4j.LoggerFactory
import org.apache.commons.lang3.time.DateFormatUtils
import org.springframework.data.jpa.domain.Specifications
import javax.persistence.criteria.CriteriaBuilder
import javax.persistence.criteria.CriteriaQuery
import javax.persistence.criteria.JoinType
import javax.persistence.criteria.Root
import javax.persistence.criteria.Join

import java.time.Instant
import java.util.regex.Pattern

import static groovy.json.JsonOutput.toJson
import static groovyx.net.http.ContentType.JSON
import static groovyx.net.http.Method.POST

XM_ENTITY_KEY_PATTERN = Pattern.compile("([a-zA-Z0-9.-]+)-(\\d+)")

log = LoggerFactory.getLogger(getClass())
xmEntityService = lepContext.services.xmEntity
linkService = lepContext.services.linkService

//--------------------- MAIN -----------------------------------------------------
xmEntity = lepContext.inArgs.xmEntity
isNewEntity = (xmEntity?.id == null)

//BEFORE
if (isAddStationCase()) enrichStation(xmEntity)
if (isAddPersonalCarCase()) beforeSavePersonalCar(lepContext, xmEntity)
if (isChargeStationCase()) updateChargeStation(xmEntity)
if (isUpdateCarAeCase()) updateCarAe(xmEntity)
if (isCarAeCase()) enrichCarAe(xmEntity)

// AROUND
if (isStartChargeCase()) return startCharge(xmEntity)
if (isOrderCarChargeServiceCase()) return orderCarChargeService(xmEntity)
if (isOrderCarShareServiceCase()) return orderCarShareService(xmEntity)
if (isOrderCarRentCase()) return orderCarRent(xmEntity)

xmEntity = lepContext.lep.proceed(xmEntity)

//AFTER
if (isAddPersonalCarCase()) addPersonalCar()
if (isNewAccount()) orderCarChargeService(xmEntity)

return xmEntity

//--------------------- CONDITION ---------------------------------------------------
def isAddStationCase() {
    isNewEntity && xmEntity?.typeKey == 'RESOURCE.CHARGING-STATION'
}

def isAddPersonalCarCase() {
    isNewEntity && xmEntity?.typeKey == 'RESOURCE.CAR.PERSONAL'
}

def isStartChargeCase() {
    xmEntity?.typeKey == 'ORDER.AEPRODUCT' &&
            !xmEntity?.targets?.isEmpty() &&
            xmEntity?.targets?.first()?.typeKey == 'LINK.ORDER.OFFERING' &&
            xmEntity?.targets?.first()?.target?.typeKey?.startsWith('OFFERING.AEPRODUCT.CAR-CHARGE.AECHARGE')
}

def isOrderCarChargeServiceCase() {
    isNewEntity && xmEntity?.typeKey == 'ORDER.SERVICE' &&
            !xmEntity?.targets?.isEmpty() &&
            xmEntity?.targets?.first()?.typeKey == 'LINK.ORDER.OFFERING' &&
            xmEntity?.targets?.first()?.target?.typeKey?.startsWith('OFFERING.SERVICE.CAR-CHARGING')
}

def isOrderCarShareServiceCase() {
    isNewEntity && xmEntity?.typeKey == 'ORDER.SERVICE' &&
            !xmEntity?.targets?.isEmpty() &&
            xmEntity?.targets?.first()?.typeKey == 'LINK.ORDER.OFFERING' &&
            xmEntity?.targets?.first()?.target?.typeKey?.startsWith('OFFERING.SERVICE.CAR-SHARING')
}

def isUpdateCarAeCase() {
    !isNewEntity && xmEntity?.typeKey == 'RESOURCE.CAR.AE'
}

def isChargeStationCase() {
    xmEntity?.typeKey == 'RESOURCE.CHARGING-STATION'
}

def isCarAeCase() {
    xmEntity?.typeKey == 'RESOURCE.CAR.AE'
}

def isOrderCarRentCase() {
    isNewEntity && xmEntity?.typeKey == 'ORDER.AEPRODUCT' &&
            !xmEntity?.targets?.isEmpty() &&
            xmEntity?.targets?.first()?.typeKey == 'LINK.ORDER.OFFERING' &&
            xmEntity?.targets?.first()?.target?.typeKey?.startsWith('OFFERING.AEPRODUCT.CAR-RENT')
}

def isNewAccount() {
    isNewEntity && xmEntity?.typeKey?.startsWith('ACCOUNT')
}

//--------------------- BEFORE EXECUTION function -----------------------------------
def enrichStation(XmEntity xmEntity) {
    def stationType = xmEntity?.data?.stationType

    xmEntity.avatarUrl = 'http://mock.rgw.icthh.com/'
    if (stationType == 'CHAdeMO') {
        xmEntity.avatarUrl += '9ff2d920-9c4a-4eef-89bd-031747f32f22.png'
    } else if (stationType == 'J1772') {
        xmEntity.avatarUrl += 'e4b49608-fcc9-445d-a520-f413c4fae443.png'
    } else {
        xmEntity.avatarUrl += 'c633bbb7-1802-471a-83d9-3718fd30655b.png'
    }
}

def beforeSavePersonalCar(def lepContext, XmEntity xmEntity) {
    XmEntityService xmEntityService = lepContext.services.xmEntity

    vin = xmEntity.data.vin
    def car = xmEntityService.findAll(Specifications.where({ Root root, CriteriaQuery query, CriteriaBuilder cb ->
        return cb.and(cb.equal(root.get("key"), vin), cb.equal(root.get("typeKey"), "RESOURCE.CAR.PERSONAL"))
    }))

    if (car) throw new BusinessException('ECS-0017', "Car with vin code ${vin} already exists")

    //enrich personal car entity
    date = Instant.now()
    xmEntity.setName("Personal car")
    xmEntity.setStartDate(date)
    xmEntity.setUpdateDate(date)
    xmEntity.setStateKey('AVAILABLE')
    xmEntity.setKey(vin)
}

//--------------------- AFTER EXECUTION functions -----------------------------------
def addPersonalCar() {
    addPersonalCarLinks(lepContext, xmEntity)
    createPersonalCarOnAe(lepContext, xmEntity)
}

def addPersonalCarLinks(def lepContext, XmEntity xmEntity) {
    Link link = new Link(
            'name': 'Car owner',
            'typeKey': 'LINK.ACCOUNT.RESOURCE.CAR',
            'startDate': new Date().toInstant(),
            'source': getCurrentUserEntity(),
            'target': xmEntity)

    lepContext.services.linkService.save(link)
}

def createPersonalCarOnAe(def lepContext, XmEntity xmEntity) {
    def pathToEcsApi = lepContext.services.tenantConfigService.config?.integration?.aeApiUrl

    def accessToken = getAccessToken()
    def http = new HTTPBuilder(pathToEcsApi + "/updateResource?access_token=${accessToken}")
    http.ignoreSSLIssues()

    http.request(POST, JSON) {
        body = buildCarCreateRequest(xmEntity)
        response.failure = { resp, json ->
            log.info('Response from /updateResource BODY {}', json)
            throw new BusinessException('error.external.ae.api', "${json}")
        }

        response.success = { resp, json ->
            log.info('Response from /updateResource BODY {}', json)
        }
    }
}

def buildCarCreateRequest(XmEntity xmEntity) {
    def creationDate = DateFormatUtils.ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.format(new Date())
    request = [
            source        : 'XM-BE',
            eventType     : 'RESOURCE_CAR_CREATED',
            messagingType : 'SYNC',
            eventUuid     : MdcUtils.getRid(),
            eventTimestamp: creationDate,
            entityTypeKey : 'RESOURCE.CAR.PERSONAL',
            xmResourceId  : xmEntity.id as String,
            aeAccountId   : null,
            eventData     : [
                    xmState   : 'NEW',
                    name      : xmEntity.name,
                    startDate : creationDate,
                    updateDate: creationDate,
                    entityData: [
                            registrationNumber: xmEntity.data.registrationNumber,
                            vin               : xmEntity.data.vin,
                            evColor           : xmEntity.data.evColor ?: '',
                            evModel           : xmEntity.data.evModel ?: '',
                            evBrand           : xmEntity.data.evBrand ?: ''
                    ]
            ]
    ]

    log.info('POST request to /updateResource BODY {}', toJson(request))
    return request
}

//--------------------- Start charge -----------------------------------
def startCharge(XmEntity xmEntity) {
    XmEntity product = XmEntityUtils.getRequiredLinkedTarget(xmEntity, "LINK.ORDER.PRODUCT",
            "PRODUCT.AEPRODUCT.CAR-CHARGE.AECHARGE")

    int chargingStationConnector = XmEntityUtils.getRequiredDataParam(product,
            "chargingStationConnector",
            Integer.class,
            "Ordering product has no required parameter: data.%s")

    //1. Get ChargingStation key (id)
    XmEntity chargingStation = XmEntityUtils
            .getRequiredLinkedTarget(xmEntity, "LINK.ORDER.RESOURCE.CHARGING-STATION",
            "RESOURCE.CHARGING-STATION")
    String aeChargingStationId = chargingStation.key

    //2. Get Car key (id)
    Optional<XmEntity> car = XmEntityUtils.getLinkedTarget(xmEntity, "LINK.ORDER.RESOURCE.CAR",
            "RESOURCE.CAR.PERSONAL")
    String carVinCode = car.isPresent() ? car.get().key : null

    //3. Check is car charge service ordered
    if (!getActiveCarChargeService()) orderCarChargeService(getCurrentUserEntity())
    //throw new BusinessException('Car charging service not ordered for account ' + getCurrentUserEntity().name)

    //4. Check is balance >= 0
    validateBalance()

    //5. Call AE API order AE-ProductOffering “Car charge”
    xmEntity = orderAeProductCarCharge(xmEntity,
            aeChargingStationId,
            chargingStationConnector,
            carVinCode)
    return xmEntity
}

def validateBalance() {

    Optional<BigDecimal> uahBalanceAmount = getCurrentBalanceAmount("UAH")
    if (!uahBalanceAmount.isPresent()) {
        // TODO add custom error localization support in xm-ms-entity
        throw new BusinessException("error.emptyBalance",
                "Sorry, your have empty balance and can't start charge")
    }

    if (uahBalanceAmount.get().compareTo(BigDecimal.ZERO) < 0) {
        // TODO add custom error localization support in xm-ms-entity
        throw new BusinessException("error.notEnoughBalance",
                "Sorry, the balance is not not enough to start charging")
    }
}

def getCurrentBalanceAmount(String aeCurrency) {
    def pathToAeApi = lepContext.services.tenantConfigService.config?.integration?.aeApiUrl

    def accessToken = getAccessToken();

    def http = new HTTPBuilder(pathToAeApi + "/getAccount?access_token=${accessToken}")
    http.ignoreSSLIssues()

    http.request(Method.GET, JSON) {
        response.failure = { resp, json ->
            log.info('Response from /getAccount BODY {}', json)
            throw new BusinessException('error.external.ae.api', "${json}")
        }

        response.success = { resp, json ->
            log.info('Response from /getAccount BODY {}', json)
            return extractBalance(json, aeCurrency)
        }
    }
}

def extractBalance(Map account, String aeCurrency) {
    // filter balances with type Line_Main and currency UAH only
    List balances = account?.get("balances")?.findAll {
        balance ->
            def unitOfMeasure = Objects.requireNonNull(balance.get("unitOfMeasure"),
                    "AE unitOfMeasure is required but null")
            def balanceType = Objects.requireNonNull(balance.get("type"),
                    "AE balance.type is required but null")
            Objects.requireNonNull(balance.get("amount"), "AE balance amount can't be null")

            return "Line_Main".equals(balanceType.get("code")) &&
                    "currency".equals(unitOfMeasure.get("type")) &&
                    Objects.equals(unitOfMeasure.get("value"), aeCurrency)
    }

    if (balances.isEmpty()) {
        return Optional.empty()
    }

    BigDecimal sum = BigDecimal.ZERO
    for (Map balance : balances) {
        sum = sum.add(new BigDecimal(balance.get("amount")))
    }
    return Optional.of(sum)
}

def getRequiredAeStationId(String stationId) {
    XmEntityService xmEntityService = lepContext.services.xmEntity
    XmEntity chargingStation = xmEntityService.findOne(IdOrKey.of(stationId))

    if (!chargingStation) throw new BusinessException("Station not found by key ${stationId}");

    String stationLogicalId = (String) chargingStation.getData().get("stationLogicalId")
    return Integer.parseInt(stationLogicalId)
}

def getRequiredCarVin(String carId) {
    XmEntityService xmEntityService = lepContext.services.xmEntity
    XmEntity resourceCar = xmEntityService.findOne(IdOrKey.of(carId))

    if (resourceCar == null) {
        throw new BusinessException("Car not found by key: " + carId)
    }
    return XmEntityUtils.getRequiredDataParam(resourceCar, "vin", String.class)
}

def getOrderOffering(XmEntity aeProductOrder) {
    return XmEntityUtils.getRequiredTargetByLink(aeProductOrder, "LINK.ORDER.OFFERING")
}

def orderAeProductCarCharge(XmEntity xmEntity,
                            String stationId,
                            int chargingStationConnector,
                            String carId) {
    def pathToAeApi = lepContext.services.tenantConfigService.config?.integration?.aeApiUrl
    def accessToken = getAccessToken();
    def http = new HTTPBuilder(pathToAeApi + "/order?access_token=${accessToken}")
    http.ignoreSSLIssues()

    http.request(POST, JSON) {
        body = buildAeProductCarChargeRequest(xmEntity,
                chargingStationConnector,
                stationId,
                carId)
        response.failure = { resp, json ->
            log.info('Response from /order BODY {}', json)
            throw new BusinessException('error.external.ae.api', "${json}")
        }

        response.success = { resp, json ->
            log.info('Response from /order BODY {}', json)
            XmEntity responseOrder = new XmEntity()
            responseOrder.setId(-1L) // fake id
            responseOrder.setTypeKey(json.get("entityTypeKey"))
            return responseOrder
        }
    }
}

def buildAeProductCarChargeRequest(XmEntity xmEntity,
                                   int chargingStationConnector,
                                   String xmStationId,
                                   String xmCarId) {
    def creationDate = DateFormatUtils.ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.format(new Date())

    XmEntity offering = getOrderOffering(xmEntity)
    String offeringId = keyToIdStr(offering, XM_ENTITY_KEY_PATTERN)

    int aeStationId = getRequiredAeStationId(xmStationId)
    String vin = getRequiredCarVin(xmCarId)

    request = [
            source        : 'XM-BE',
            eventType     : 'ORDER_CREATE',
            messagingType : 'SYNC',
            eventUuid     : MdcUtils.getRid(),
            eventTimestamp: creationDate,
            entityTypeKey : 'ORDER.AEPRODUCT',
            eventData     : [
                    offeringTypeKey    : offering.getTypeKey(),
                    xmOfferingId       : offeringId,
                    aeStationResourceId: aeStationId,
                    xmStationResourceId: xmStationId,
                    connectorNumber    : chargingStationConnector,
                    xmCarResourceId    : xmCarId,
                    vin                : vin
            ]
    ]

    LoggerFactory.getLogger(getClass()).info('POST request to /order BODY {}', toJson(request))
    return request
}

//--------------------- ORDER CAR CHARGE SERVICE -----------------------------------
def orderCarChargeService(XmEntity xmEntity) {
    currentAccount = xmEntity.typeKey.startsWith("ACCOUNT") ? xmEntity : getCurrentUserEntity()
    log.info("Order car charge service for ${xmEntity.name}")

    if (getActiveCarChargeServiceForUser(currentAccount.id))
        throw new BusinessException('Car charging service already ordered for account ' + currentAccount.name)

    creationDate = Instant.now()
    serviceProduct = xmEntityService.save(new XmEntity(
            "typeKey": 'PRODUCT.SERVICE.CAR-CHARGING.SUBSCRIBER-CHARGE100',
            "key": 'PRODUCT.SERVICE.CAR-CHARGING.SUBSCRIBER-CHARGE100-XXX',
            "stateKey": 'ACTIVE',
            "startDate": creationDate,
            "updateDate": creationDate,
            "name": 'Car charging service for account ' + currentAccount.name
    ))

    //update key
    serviceProduct.setKey(serviceProduct.id.toString())
    xmEntityService.save(serviceProduct)

    addLink('Car charge service', 'LINK.ACCOUNT.PRODUCT.SERVICE', currentAccount, serviceProduct)
    addLink('Account ordered service', 'LINK.PRODUCT.ACCOUNT.SERVICE', serviceProduct, currentAccount)

    return new XmEntity(typeKey: 'ORDER.SERVICE',
            targets: [new Link(
                    typeKey: 'LINK.ORDER.PRODUCT',
                    target: new XmEntity(key: serviceProduct.key,
                            typeKey: 'PRODUCT.SERVICE.CAR-CHARGING.SUBSCRIBER-CHARGE100')
            )])
}

//--------------------- ORDER CAR SHARE SERVICE -----------------------------------
def orderCarShareService(XmEntity xmEntity) {
    if (getActiveCarShareService())
        throw new BusinessException('Car sharing service already ordered for account ' + getCurrentUserEntity().name)

    linkOrderProduct = XmEntityUtils.getRequiredLinkedTarget(xmEntity, "LINK.ORDER.PRODUCT",
            "PRODUCT.SERVICE.CAR-SHARING.DEFAULT-CARSHARE")

    creationDate = Instant.now()
    serviceProduct = xmEntityService.save(new XmEntity(
            "typeKey": 'PRODUCT.SERVICE.CAR-SHARING.DEFAULT-CARSHARE',
            "key": 'PRODUCT.SERVICE.CAR-SHARING.DEFAULT-CARSHARE-XXX',
            "stateKey": 'NEW',
            "startDate": creationDate,
            "updateDate": creationDate,
            "name": 'Car sharing service for account ' + getCurrentUserEntity().name,
            "data": [
                    "birthday": linkOrderProduct.data.birthday,
                    "city"    : linkOrderProduct.data.city,
                    "address" : linkOrderProduct.data.address
            ]
    ))

    //update key
    serviceProduct.setKey(serviceProduct.id.toString())
    xmEntityService.save(serviceProduct)

    addLink('Car share service', 'LINK.ACCOUNT.PRODUCT.SERVICE', getCurrentUserEntity(), serviceProduct)
    addLink('Account ordered service', 'LINK.PRODUCT.ACCOUNT.SERVICE', serviceProduct, getCurrentUserEntity())

    currentAccount = getCurrentUserEntity()
    currentAccount.getData().put("address", linkOrderProduct.data.address)
    currentAccount.getData().put("city", linkOrderProduct.data.city)
    currentAccount.getData().put("dateOfBirth", linkOrderProduct.data.birthday)

    // currentAccount contains  sources and targets. Some error present during save
    currentAccount.sources.clear()
    currentAccount.targets.clear()
    currentAccount.functionContexts.clear()
    xmEntityService.save(currentAccount)

    //todo send notification to all dispatchers
//    accountService.sendEmail(
//            invar.tenant,
//            tenantName + "@icthh.com",
//            dispatcherEmails,
//            'EcoCarSharing: New user registration',
//            "Через приложение EcoCarSharing зарегистрирован новый пользователь: " + displayName + ". " +
//                    "Для завершения процесса регистрации Вы должны в течение 24х часов проверить
//                     персональные данные и активировать учетную запись пользователя.",
//            "RU"
//    )

    return new XmEntity(typeKey: 'ORDER.SERVICE',
            targets: [new Link(
                    typeKey: 'LINK.ORDER.PRODUCT',
                    target: new XmEntity(key: serviceProduct.key,
                            typeKey: 'PRODUCT.SERVICE.CAR-SHARING.DEFAULT-CARSHARE')
            )])
}

//--------------------- UPDATE CAR AE  -----------------------------
def updateCarAe(XmEntity xmEntity) {
    updateLocations(xmEntity.locations)
}

//--------------------- UPDATE CHARGE STATION ---------------------------
def updateChargeStation(XmEntity xmEntity) {
    foundStations = xmEntityService.findAll(Specifications.where({ Root root, CriteriaQuery query, CriteriaBuilder cb ->
        root.fetch("locations")
        return cb.and(
                cb.equal(root.get("key"), xmEntity.key),
                cb.equal(root.get("typeKey"), "RESOURCE.CHARGING-STATION")
        )
    }))

    if (foundStations) {
        //station already exist by key, enrich with ID's for update
        foundStation = foundStations.first()
        xmEntity.id = foundStation.id

        location = xmEntity.locations.first()

        location.id = foundStation.locations.first().id
        location.xmEntity = xmEntity

        updateLocations(xmEntity.locations)
    }
}

//--------------------- ORDER CAR RENT -----------------------------------
def orderCarRent(XmEntity xmEntity) {
    //1 check active service
    if (!getActiveCarShareService())
        throw new BusinessException('Car sharing service is not activated for account ' + getCurrentUserEntity().name)

    //2 check car for rent
    XmEntity foundCar = getCarForRent()

    //3 check active rents
    foundActiveRents = getActiveCarRents()
    if (foundActiveRents) throw new BusinessException("ECS-0004", "Found active rent with id " +
            foundActiveRents.first().id)

    //4 create product
    creationDate = Instant.now()
    XmEntity resultOrder = xmEntityService.save(new XmEntity(
            key: "ae-product-???",
            typeKey: "PRODUCT.AEPRODUCT.CAR-RENT.PER-MIN-RENT",
            name: "Car rent for " + getCurrentUserEntity().name,
            stateKey: "ACTIVE",
            startDate: creationDate,
            updateDate: creationDate,
            data: [:]
    ))

    //5 create links
    // product -> car
    prodcutCarLink = addLink('Rented car', 'LINK.ACCOUNT.RESOURCE.CAR', resultOrder, foundCar)
    // car -> account
    addLink('Renter', 'RENTER', foundCar, getCurrentUserEntity())
    // account -> product
    addLink('Rents', 'LINK.ACCOUNT.PRODUCT.AEPRODUCT', getCurrentUserEntity(), resultOrder)

    //6 change car state to RESERVED
    xmEntityService.updateState(IdOrKey.of(foundCar.id), 'RESERVED', [:])

    //7 AE notification
    aeCarRentProductId = aeCarRentProductCreated(resultOrder.id, foundCar.id, foundCar.data.vin)

    //8 rent product update
    resultOrder.key = "ae-product-" + aeCarRentProductId
    resultOrder.data.aeProductId = aeCarRentProductId
    xmEntityService.save(resultOrder)

    return new XmEntity(
            typeKey: "ORDER.AEPRODUCT",
            targets: [
                    new Link(
                            typeKey: "LINK.ORDER.PRODUCT",
                            target: new XmEntity(
                                    key: resultOrder.key,
                                    typeKey: resultOrder.typeKey
                            )
                    )
            ]
    )
}

def getActiveCarRents() {
    return linkService.findAll(Specifications.where({ Root root, CriteriaQuery query, CriteriaBuilder cb ->
        Join<XmEntity, Link> source = root.join("source", JoinType.INNER)
        Join<XmEntity, Link> target = root.join("target", JoinType.INNER)

        return cb.and(
                cb.equal(source.get("id"), getCurrentUserEntity().getId()),
                cb.like(target.get("typeKey"), "PRODUCT.AEPRODUCT.CAR-RENT%"),
                cb.not(target.get("stateKey").in(
                        "CANCELED",
                        "RENT_FINISHED_OTHER_PLACE",
                        "RENT_FINISHED_OCCUPIED",
                        "RENT_FINISHED")
                )
        )
    })).collect({ it -> it.target })
}

def getCarForRent() {
    XmEntity carForRent = XmEntityUtils.getRequiredLinkedTarget(xmEntity, "LINK.ORDER.RESOURCE.CAR", "RESOURCE.CAR.AE")

    findResults = xmEntityService.findAll(Specifications.where({ Root root, CriteriaQuery query, CriteriaBuilder cb ->
        return cb.and(cb.equal(root.get("key"), carForRent.key), cb.equal(root.get("typeKey"), "RESOURCE.CAR.AE"))
    }))

    if (!findResults) throw new BusinessException("Cant find car for vin:${carForRent.key}")
    foundCar = findResults.first()
    if ('RESERVED'.equals(foundCar.stateKey))
        throw new BusinessException("ECS-0001", "${foundCar.name} : ${foundCar.key} is reserved")
    if (!'AVAILABLE'.equals(foundCar.getStateKey()))
        throw new BusinessException("ECS-0002", "${foundCar.name} : ${foundCar.key} is not available")

    return foundCar
}

def aeCarRentProductCreated(productId, carId, vin) {
    def pathToAeApi = lepContext.services.tenantConfigService.config?.integration?.aeApiUrl

    http = new HTTPBuilder(pathToAeApi + '/updateProduct?access_token=' + getAccessToken())
    http.ignoreSSLIssues()

    def request = [
            source        : 'XM-BE',
            eventType     : 'PRODUCT_AEPRODUCT_CREATED',
            messagingType : 'SYNC',
            eventUuid     : MdcUtils.getRid(),
            eventTimestamp: DateFormatUtils.ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.format(new Date()),
            entityTypeKey : 'PRODUCT.AEPRODUCT.CAR-RENT.PER-MIN-RENT',
            xmProductId   : productId,
            aeProductId   : null,
            eventData     : [
                    xmState   : 'ACTIVE',
                    entityData: [
                            xmCarResourceId: carId,
                            aeCarResourceId: null,
                            vin            : vin
                    ]
            ]
    ]
    log.info('POST request to /updateProduct BODY {}', toJson(request))

    http.request(POST, JSON) {
        body = request
        response.success = {
            resp, json ->
                log.info('Response from /updateProduct BODY {}', json)
                String aeProductId = json.aeProductId.toString()
                return aeProductId
        }
        response.failure = {
            resp ->
                log.error('Response from /updateProduct BODY ${resp.statusLine}')
                throw new BusinessException('error.external.ae.api', "${resp.statusLine}")
        }
    }
}

def enrichCarAe(XmEntity xmEntity) {
    xmEntity.key = xmEntity.data.vin
    xmEntity.avatarUrl = 'http://mock.rgw.icthh.com/'

    switch (xmEntity.data.evBrand) {
        case 'Nissan':
            switch (xmEntity.data.evModel) {
                case 'Leaf':
                    switch (xmEntity.data.evColor) {
                        case 'Brilliant Silver':
                            // Nissan Leaf - Brilliant Silver
                            xmEntity.avatarUrl += "02f2a82a-3e97-4103-b043-6f35e8f8765d.jpg"
                            break;
                        case 'Coulis Red':
                            // Nissan Leaf - Coulis Red
                            xmEntity.avatarUrl += "27fa9238-3f62-4703-84fe-2addb6b325b9.jpg"
                            break
                        case 'Deep Blue Pearl':
                            // Nissan Leaf - Deep Blue Pearl
                            xmEntity.avatarUrl += "fe5a6797-ea03-4a77-862f-6e0c4f945166.jpg"
                            break
                        case 'Forged Bronze':
                            // Nissan Leaf - Forged Bronze
                            xmEntity.avatarUrl += "d2508f1d-8444-43e4-9a22-363052565db8.jpg"
                            break
                        case 'Glacier White':
                            // Nissan Leaf - Glacier White
                            xmEntity.avatarUrl += "74c3b227-7d67-42c3-8510-54d8831c5858.jpg"
                            break
                        case 'Pearl White':
                            // Nissan Leaf - Pearl White
                            xmEntity.avatarUrl += "2a3c7434-e523-4f67-851d-b7a533b270d7.jpg"
                            break
                        case 'Super Black':
                            // Nissan Leaf - Super Black
                            xmEntity.avatarUrl += "c1207842-ba3f-46a1-8d88-a82bc064ef5d.jpg"
                            break
                        case 'Gun Metallic':
                            // Nissan Leaf - Gun Metallic
                            xmEntity.avatarUrl += "f6ff4cf1-4a75-4cb8-8aac-fbdb3c6990cb.jpg"
                            break
                        default:
                            // Nissan Leaf - Unknown Color
                            xmEntity.avatarUrl += "11fb4454-ca4d-4620-bafc-ee46e95394c3.jpg"
                            break
                    }
            }
    }
}
//--------------------- HELPERS -----------------------------------
def getAccessToken() {
    return lepContext.authContext.getAdditionalDetailsValue('aeAccessToken').orElseThrow({
        throw new BusinessException('error.external.ae.api', 'AE access token not found for this account')
    })
}

def getCurrentUserEntity() {
    return lepContext.services.profileService.getSelfProfile().xmentity
}

def keyToIdStr(XmEntity xmEntity, Pattern pattern) {
    String selfAwareKey = Constants.PATH_SELF.equals(xmEntity.getKey()) ? "ACCOUNT-0" : xmEntity.getKey()
    XmEntityKey entityKey = XmEntityKey.ofMatcher(selfAwareKey, pattern)
    return entityKey.getGroup(1)
}

def addLink(String name, String type, XmEntity source, XmEntity target) {
    Link link = new Link(
            'name': name,
            'typeKey': type,
            'startDate': new Date().toInstant(),
            'source': source,
            'target': target)
    lepContext.services.linkService.save(link)
}

def getActiveCarChargeService() {
    return getActiveService("PRODUCT.SERVICE.CAR-CHARGING%", getCurrentUserEntity().getId())
}

def getActiveCarChargeServiceForUser(accountId) {
    return getActiveService("PRODUCT.SERVICE.CAR-CHARGING%", accountId)
}

def getActiveCarShareService() {
    return getActiveService("PRODUCT.SERVICE.CAR-SHARING%", getCurrentUserEntity().getId())
}

def getActiveService(name, accountId) {
    links = linkService.findAll(Specifications.where({ Root root, CriteriaQuery query, CriteriaBuilder cb ->
        Join<XmEntity, Link> source = root.join("source", JoinType.INNER)
        Join<XmEntity, Link> target = root.join("target", JoinType.INNER)
        return cb.and(cb.equal(source.get("id"), accountId),
                cb.like(target.get("typeKey"), name),
                cb.equal(target.get("stateKey"), "ACTIVE"))
    }))

    return links
}

def updateLocations(locations) {
    def location = lepContext.services.locationService
    locations?.each {
        location.save(it)
    }
}