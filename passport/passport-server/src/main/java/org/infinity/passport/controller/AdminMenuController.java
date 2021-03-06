package org.infinity.passport.controller;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.validation.Valid;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.infinity.passport.domain.AdminMenu;
import org.infinity.passport.domain.Authority;
import org.infinity.passport.dto.AdminMenuDTO;
import org.infinity.passport.exception.FieldValidationException;
import org.infinity.passport.exception.NoDataException;
import org.infinity.passport.repository.AdminMenuRepository;
import org.infinity.passport.service.AdminMenuService;
import org.infinity.passport.utils.HttpHeaderCreator;
import org.infinity.passport.utils.PaginationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.codahale.metrics.annotation.Timed;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

/**
 * REST controller for managing the admin menu.
 */
@RestController
@Api(tags = "管理菜单")
public class AdminMenuController {

    private static final Logger LOGGER = LoggerFactory.getLogger(AdminMenuController.class);

    @Autowired
    private AdminMenuRepository adminMenuRepository;

    @Autowired
    private AdminMenuService    adminMenuService;

    @Autowired
    private HttpHeaderCreator   httpHeaderCreator;

    @ApiOperation("创建菜单")
    @ApiResponses(value = { @ApiResponse(code = 201, message = "成功创建") })
    @PostMapping("/api/admin-menu/menus")
    @Secured({ Authority.ADMIN })
    @Timed
    public ResponseEntity<Void> create(
            @ApiParam(value = "菜单信息", required = true) @Valid @RequestBody AdminMenuDTO dto) {
        LOGGER.debug("REST request to create admin menu: {}", dto);
        adminMenuRepository.findOneByAppNameAndSequence(dto.getAppName(), dto.getSequence())
                .ifPresent((existingEntity) -> {
                    throw new FieldValidationException("adminMenuDTO", "appName+sequence",
                            MessageFormat.format("appName: {0}, sequence: {1}", dto.getAppName(), dto.getSequence()),
                            "error.duplication",
                            MessageFormat.format("appName: {0}, sequence: {1}", dto.getAppName(), dto.getSequence()));
                });
        adminMenuService.insert(dto.getAppName(), dto.getAdminMenuName(), dto.getAdminMenuChineseText(), dto.getLink(),
                dto.getSequence(), dto.getParentMenuId());
        return ResponseEntity.status(HttpStatus.CREATED).headers(
                httpHeaderCreator.createSuccessHeader("notification.admin.menu.created", dto.getAdminMenuName()))
                .build();
    }

    @ApiOperation("获取菜单信息分页列表")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "成功获取") })
    @GetMapping("/api/admin-menu/menus")
    @Secured({ Authority.ADMIN })
    @Timed
    public ResponseEntity<List<AdminMenuDTO>> getMenus(Pageable pageable,
            @ApiParam(value = "应用名称", required = false) @RequestParam(value = "appName", required = false) String appName)
            throws URISyntaxException {
        Page<AdminMenu> adminMenus = StringUtils.isEmpty(appName) ? adminMenuRepository.findAll(pageable)
                : adminMenuRepository.findByAppName(pageable, appName);
        List<AdminMenuDTO> adminMenuDTOs = adminMenus.getContent().stream().map(entity -> entity.asDTO())
                .collect(Collectors.toList());
        HttpHeaders headers = PaginationUtils.generatePaginationHttpHeaders(adminMenus, "/api/admin-menu/menus");
        return new ResponseEntity<>(adminMenuDTOs, headers, HttpStatus.OK);
    }

    @ApiOperation("根据菜单ID查询菜单")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "成功获取"), @ApiResponse(code = 400, message = "菜单不存在") })
    @GetMapping("/api/admin-menu/menus/{id}")
    @Secured({ Authority.ADMIN })
    @Timed
    public ResponseEntity<AdminMenuDTO> getMenu(@ApiParam(value = "菜单ID", required = true) @PathVariable String id) {
        AdminMenu entity = Optional.ofNullable(adminMenuRepository.findOne(id))
                .orElseThrow(() -> new NoDataException(id));
        return new ResponseEntity<>(entity.asDTO(), HttpStatus.OK);
    }

    @ApiOperation("查询父类菜单")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "成功获取") })
    @GetMapping("/api/admin-menu/parent-menus/{appName}/{level:[0-9]+}")
    @Secured({ Authority.ADMIN })
    @Timed
    public ResponseEntity<List<AdminMenuDTO>> getAllParentMenu(
            @ApiParam(value = "应用名称", required = true) @PathVariable String appName,
            @ApiParam(value = "菜单级别", required = true) @PathVariable Integer level) {
        List<AdminMenuDTO> dtos = adminMenuRepository.findByAppNameAndLevel(appName, level).stream()
                .map(adminMenu -> adminMenu.asDTO()).collect(Collectors.toList());
        return new ResponseEntity<List<AdminMenuDTO>>(dtos, HttpStatus.OK);
    }

    @ApiOperation("更新菜单")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "成功更新"), @ApiResponse(code = 400, message = "菜单不存在") })
    @PutMapping("/api/admin-menu/menus")
    @Secured({ Authority.ADMIN })
    @Timed
    public ResponseEntity<Void> update(
            @ApiParam(value = "新的菜单信息", required = true) @Valid @RequestBody AdminMenuDTO dto) {
        LOGGER.debug("REST request to update admin menu: {}", dto);
        Optional.ofNullable(adminMenuRepository.findOne(dto.getId()))
                .orElseThrow(() -> new NoDataException(dto.getId()));

        adminMenuService.update(dto.getId(), dto.getAppName(), dto.getAdminMenuName(), dto.getAdminMenuChineseText(),
                dto.getLevel(), dto.getLink(), dto.getSequence(), dto.getParentMenuId());
        return ResponseEntity.status(HttpStatus.OK).headers(
                httpHeaderCreator.createSuccessHeader("notification.admin.menu.updated", dto.getAdminMenuName()))
                .build();
    }

    @ApiOperation("根据应用名称和菜单ID删除管理菜单")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "成功删除"), @ApiResponse(code = 400, message = "菜单不存在") })
    @DeleteMapping("/api/admin-menu/menus/{id}")
    @Secured({ Authority.ADMIN })
    @Timed
    public ResponseEntity<Void> delete(@ApiParam(value = "菜单ID", required = true) @PathVariable String id) {
        LOGGER.debug("REST request to delete admin menu: {}", id);
        AdminMenu adminMenu = Optional.ofNullable(adminMenuRepository.findOne(id))
                .orElseThrow(() -> new NoDataException(id));
        adminMenuRepository.delete(id);
        return ResponseEntity.status(HttpStatus.OK).headers(
                httpHeaderCreator.createSuccessHeader("notification.admin.menu.deleted", adminMenu.getAdminMenuName()))
                .build();
    }

    @ApiOperation(value = "导入管理菜单", notes = "输入文件格式：每行先后appName,adminMenuName,adminMenuChineseText,level,link,sequence数列，列之间使用tab分隔，行之间使用回车换行")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "成功导入") })
    @PostMapping(value = "/api/admin-menu/menus/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Secured({ Authority.ADMIN })
    @Timed
    public ResponseEntity<Void> importData(@ApiParam(value = "文件", required = true) @RequestPart MultipartFile file)
            throws IOException, InterruptedException {
        List<String> lines = IOUtils.readLines(file.getInputStream(), StandardCharsets.UTF_8);
        List<AdminMenu> list = new ArrayList<AdminMenu>();
        for (String line : lines) {
            if (StringUtils.isNotEmpty(line)) {
                String[] lineParts = line.split("\t");

                AdminMenu entity = new AdminMenu(lineParts[0], lineParts[1], lineParts[2],
                        Integer.parseInt(lineParts[3]), lineParts[4], Integer.parseInt(lineParts[5]), null);
                list.add(entity);
            }
        }
        adminMenuRepository.insert(list);
        return new ResponseEntity<>(HttpStatus.OK);
    }
}
