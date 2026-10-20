package com.example.demo.config;

import com.example.demo.service.DatosMaestrosService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ClienteCondicionIvaMigration implements ApplicationRunner {

    private final DatosMaestrosService datosMaestrosService;

    @Override
    public void run(ApplicationArguments args) {
        datosMaestrosService.migrarCondicionIvaClientesExistentes();
    }
}
