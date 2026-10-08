package com.bezrukov.inventoryservice.repository;

import com.bezrukov.inventoryservice.entity.Product;
import org.jspecify.annotations.NullMarked;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@NullMarked
@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findAllById(Iterable<Long> ids);

    /**
     * Атомарно списывает {@code qty} со склада, только если остатка хватает.
     *
     * <p>Возвращает:
     * <ul>
     *   <li>{@code 1} — списание выполнено</li>
     *   <li>{@code 0} — товара не хватает (условие {@code quantity >= qty}
     *       не выполнено), либо товара нет в БД</li>
     * </ul>
     *
     * <p>Атомарность гарантирует Postgres: {@code UPDATE ... WHERE} блокирует
     * строку и проверяет условие в одной операции.
     */
    @Modifying
    @Query("""
            UPDATE Product p
            SET p.quantity = p.quantity - :qty
            WHERE p.id = :id AND p.quantity >= :qty
            """)
    int tryReserve(@Param("id") Long id, @Param("qty") Long qty);
}
