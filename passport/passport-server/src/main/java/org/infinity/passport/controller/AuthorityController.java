package org.infinity.passport.controller;

import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.validation.Valid;

import org.infinity.passport.domain.Authority;
import org.infinity.passport.dto.AuthorityDTO;
import org.infinity.passport.exception.NoDataException;
import org.infinity.passport.repository.AuthorityRepository;
import org.infinity.passport.utils.HttpHeaderCreator;
import org.infinity.passport.utils.PaginationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.codahale.metrics.annotation.Timed;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

/**
 * REST controller for managing authorities.
 */
@RestController
@Api(tags = "权限管理")
public class AuthorityController {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthorityController.class);

    @Autowired
    private AuthorityRepository authorityRepository;

    @Autowired
    private HttpHeaderCreator   httpHeaderCreator;

    @ApiOperation("创建权限")
    @ApiResponses(value = { @ApiResponse(code = 201, message = "成功创建") })
    @PostMapping("/api/authority/authorities")
    @Secured({ Authority.ADMIN })
    @Timed
    public ResponseEntity<Void> create(
            @ApiParam(value = "权限信息", required = true) @Valid @RequestBody AuthorityDTO dto) {
        LOGGER.debug("REST request to create authority: {}", dto);
        authorityRepository.insert(Authority.fromDTO(dto));
        return ResponseEntity.status(HttpStatus.CREATED)
                .headers(httpHeaderCreator.createSuccessHeader("notification.authority.created", dto.getName()))
                .build();
    }

    @ApiOperation("获取权限列表")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "成功获取") })
    @GetMapping("/api/authority/authorities")
    @Secured({ Authority.ADMIN })
    @Timed
    public ResponseEntity<List<AuthorityDTO>> getAuthorities(Pageable pageable) throws URISyntaxException {
        Page<Authority> authorities = authorityRepository.findAll(pageable);
        List<AuthorityDTO> authorityDTOs = authorities.getContent().stream().map(auth -> auth.asDTO())
                .collect(Collectors.toList());
        HttpHeaders headers = PaginationUtils.generatePaginationHttpHeaders(authorities, "/api/authority/authorities");
        return new ResponseEntity<>(authorityDTOs, headers, HttpStatus.OK);
    }

    @ApiOperation("获取所有权限")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "成功获取") })
    @GetMapping("/api/authority/authorities/all")
    @Secured({ Authority.ADMIN })
    @Timed
    public ResponseEntity<List<AuthorityDTO>> getAllAuthorities() {
        List<AuthorityDTO> authDTOs = authorityRepository.findAll().stream().map(entity -> entity.asDTO())
                .collect(Collectors.toList());
        return new ResponseEntity<>(authDTOs, HttpStatus.OK);
    }

    @ApiOperation("根据权限名称检索权限信息")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "成功获取"), @ApiResponse(code = 400, message = "权限信息不存在") })
    @GetMapping("/api/authority/authorities/{name}")
    @Secured({ Authority.ADMIN })
    @Timed
    public ResponseEntity<AuthorityDTO> getAuthority(
            @ApiParam(value = "权限名称", required = true) @PathVariable String name) {
        Authority authority = Optional.ofNullable(authorityRepository.findOne(name))
                .orElseThrow(() -> new NoDataException(name));
        return new ResponseEntity<>(authority.asDTO(), HttpStatus.OK);
    }

    @ApiOperation("更新权限信息")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "成功更新"), @ApiResponse(code = 400, message = "权限信息不存在") })
    @PutMapping("/api/authority/authorities")
    @Secured({ Authority.ADMIN })
    @Timed
    public ResponseEntity<Void> update(
            @ApiParam(value = "新的权限信息", required = true) @Valid @RequestBody AuthorityDTO dto) {
        LOGGER.debug("REST request to update authority: {}", dto);
        Optional.ofNullable(authorityRepository.findOne(dto.getName()))
                .orElseThrow(() -> new NoDataException(dto.getName()));
        authorityRepository.save(Authority.fromDTO(dto));
        return ResponseEntity.status(HttpStatus.OK)
                .headers(httpHeaderCreator.createSuccessHeader("notification.authority.updated", dto.getName()))
                .build();
    }

    @ApiOperation(value = "根据权限名称删除权限信息", notes = "数据有可能被其他数据所引用，删除之后可能出现一些问题")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "成功删除"), @ApiResponse(code = 400, message = "权限信息不存在") })
    @DeleteMapping("/api/authority/authorities/{name}")
    @Secured({ Authority.ADMIN })
    @Timed
    public ResponseEntity<Void> delete(@ApiParam(value = "权限名称", required = true) @PathVariable String name) {
        LOGGER.debug("REST request to delete authority: {}", name);
        if (authorityRepository.findOne(name) == null) {
            throw new NoDataException(name);
        }
        authorityRepository.delete(name);
        return ResponseEntity.status(HttpStatus.OK)
                .headers(httpHeaderCreator.createSuccessHeader("notification.authority.deleted", name)).build();
    }
}
