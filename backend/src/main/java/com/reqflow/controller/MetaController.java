package com.reqflow.controller;

import com.reqflow.common.Result;
import com.reqflow.service.MetaService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/meta")
@RequiredArgsConstructor
public class MetaController {

    private final MetaService metaService;

    @GetMapping("/options")
    public Result<Map<String, Object>> options() {
        return Result.ok(metaService.options());
    }
}
