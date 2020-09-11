package com.icthh.xm.ms.entity.web.rest;

import com.codahale.metrics.annotation.Timed;
import com.icthh.xm.commons.exceptions.BusinessException;
import com.icthh.xm.commons.logging.LoggingAspectConfig;
import com.icthh.xm.commons.permission.annotation.PrivilegeDescription;
import com.icthh.xm.ms.entity.service.ExportImportService;
import com.icthh.xm.ms.entity.service.dto.ExportDto;
import com.icthh.xm.ms.entity.service.dto.ImportDto;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ExportImportResource {

    private final ExportImportService exportImportService;

    @Timed
    @PreAuthorize("hasPermission({'exportDto': #exportDto}, 'XMENTITY.EXPORT')")
    @PrivilegeDescription("Privilege to export xmEntities by export specification")
    @PostMapping("/export/xm-entities")
    public ResponseEntity<byte[]> exportXmEntities(@RequestBody Set<ExportDto> exportDto) {
        byte[] media = exportImportService.exportEntities(exportDto);
        HttpHeaders headers = new HttpHeaders();
        headers.setCacheControl(CacheControl.noCache().getHeaderValue());
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.set("Content-Disposition", "attachment; filename=export.json");
        return ResponseEntity.ok().contentLength(media.length).headers(headers).body(media);
    }

    @Timed
    @PreAuthorize("hasPermission({'importDto': #importDto}, 'XMENTITY.IMPORT')")
    @PrivilegeDescription("Privilege to import xmEntities by import specification")
    @LoggingAspectConfig(inputExcludeParams = "importDto")
    @PostMapping("/import/xm-entities")
    public ResponseEntity<Void> importXmEntities(@RequestBody ImportDto importDto) {
        try {
            exportImportService.importEntities(importDto);
        } catch (Throwable e) {
            throw new BusinessException("error.import.xmentity", collectErrorMessage(e));
        }
        return ResponseEntity.ok().build();
    }

    private String collectErrorMessage(Throwable e) {
        StringBuilder stringBuilder = new StringBuilder();
        if (StringUtils.isNotBlank(e.getMessage())) {
            stringBuilder.append(e.getMessage()).append(". ");
        }
        if (e.getCause() != null) {
            stringBuilder.append(collectErrorMessage(e.getCause()));
        }
        return stringBuilder.toString();
    }

}
