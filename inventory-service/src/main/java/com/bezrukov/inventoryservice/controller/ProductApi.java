package com.bezrukov.inventoryservice.controller;

import com.bezrukov.inventoryservice.dto.ProductCreateRequest;
import com.bezrukov.inventoryservice.dto.ProductResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

public interface ProductApi {
    @GetMapping
    @Operation(
            summary = "Получить все товары",
            description = "Возвращает список всех товаров в каталоге"
    )
    @ApiResponse(responseCode = "200", description = "Список товаров успешно получен")
    ResponseEntity<List<ProductResponse>> findAll();

    @GetMapping("/{id}")
    @Operation(
            summary = "Получить товар по ID",
            description = "Возвращает информацию о товаре по его идентификатору"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Товар найден"),
            @ApiResponse(responseCode = "404", description = "Товар не найден")
    })
    ResponseEntity<ProductResponse> findById(
            @Parameter(description = "ID товара", required = true, example = "1")
            @PathVariable Long id
    );

    @PostMapping
    @Operation(
            summary = "Создать новый товар",
            description = "Добавляет новый товар в каталог"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Товар успешно создан"),
            @ApiResponse(responseCode = "400", description = "Ошибка валидации запроса")
    })
    ResponseEntity<ProductResponse> create(
            @Parameter(description = "Данные для создания товара", required = true)
            @Valid @RequestBody ProductCreateRequest productCreateRequest
    );

    @DeleteMapping("/{id}")
    @Operation(
            summary = "Удалить товар",
            description = "Удаляет товар по его идентификатору"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Товар успешно удален"),
            @ApiResponse(responseCode = "404", description = "Товар не найден")
    })
    ResponseEntity<Void> delete(
            @Parameter(description = "ID товара", required = true, example = "1")
            @PathVariable Long id
    );
}
