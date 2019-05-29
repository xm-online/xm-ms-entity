import com.icthh.xm.ms.entity.domain.Link
import com.icthh.xm.ms.entity.domain.XmEntity
import org.springframework.util.AntPathMatcher

import javax.servlet.http.HttpServletRequest

AntPathMatcher matcher = new AntPathMatcher()

String filter = lepContext.inArgs.filter
HttpServletRequest request = lepContext.inArgs.request
Class beanClass = lepContext.inArgs.beanClass

println "### INSIDE CustomizeFilter LEP: incoming filter = $filter"
println "### INSIDE CustomizeFilter LEP: reqest URI = ${request.requestURI}"
println "### INSIDE CustomizeFilter LEP: beanClass = $beanClass"

if (matcher.match('/api/xm-entities/{id}/links/targets', request.requestURI) && Link.class == beanClass) {
    filter = ['name', 'source', 'id'].join(',')
} else if (request.requestURI == '/api/xm-entities' && XmEntity.class == beanClass){
    filter = ['id', 'key', 'typeKey'].join(',')
}
println "### INSIDE CustomizeFilter LEP: applied filter = $filter"
filter
