import com.icthh.xm.ms.entity.domain.XmEntity
import com.icthh.xm.ms.entity.domain.ext.IdOrKey
import com.icthh.xm.ms.entity.lep.TenantLepResource
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes

import javax.servlet.ServletOutputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import static groovy.json.JsonOutput.toJson

log = LoggerFactory.getLogger(getClass())

entityService = lepContext.services.xmEntity

inData = lepContext.inArgs?.functionInput

String dateTime = inData?.currentDateTime

DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")
LocalDateTime localDateTime = LocalDateTime.parse(dateTime, dateTimeFormatter)

String dayOfWeek = localDateTime.getDayOfWeek().toString()
log.error("############################ ${dayOfWeek.toLowerCase()}")
List<XmEntity> menus = lepContext.commons.serviceWithoutLep(entityService) //  stateKey: "ACTIVE" AND "data.serviceAvailablity.dayOfWeek:\"${dayOfWeek.toLowerCase()}\" AND
    .search(" typeKey:\"MENU\"".toString(), new PageRequest(0, 9999), "XMENTITY.SEARCH")
    .getContent()
log.error("##################${menus.toArray()}")
List<Menu> menuData = new ArrayList<>();
menus.forEach({ it ->
    Menu menu = new Menu();
    log.error("##################${it}")
    menu.setId(it.getId())
    menu.setTitle(it?.data?.title)
    log.error("############ ${it.data.categories}")
    menu.setCategories(prepareCategories(it))
    menuData.add(menu)
})
response = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getResponse()
ServletOutputStream out = response.getOutputStream();
out.write(toJson(menuData).getBytes("UTF-8"))

response.setHeader("Content-Type", "application/json")
response.flushBuffer();
out.close()

return [:]

List<Category> prepareCategories(XmEntity menu) {
    List<Category> retval = new LinkedList<>();
    List<Map<String, Object>> categories = menu.data?.categories
    if (categories)
        categories.forEach({ it ->
            Category category = new Category();
            category.setId(it.category)
            category.setOrder(it.order)

            XmEntity xmEntityCategoru = lepContext.services.xmEntity.findOne(IdOrKey.of(it.category))

           /* category.setTitle(cat.data.title)
            category.setDescription(cat.data.description)
            category.setPicture(cat.data.picture)
            category.setShowprice(cat.data.showprice)*/
            retval.add(category)
        })
    return retval;
}

class Menu {
    long id;
    String title;
    List<Category> categories;

}

class Category {
    long id;
    String title;
    long order;
    String description;
    String picture;
    String showprice;
    List<Option> options;
}

class Option {
    long order;
    String title;
    List<Bar> bars;
    List<Extras> extras;
}

class Bar {
    long order;
    long id;
    String title;
    double price;
    List<Bar> bars;
}

class Extras {
    long order;
    long id;
    String title;
    double price;
    List<Extras> extras;
}
