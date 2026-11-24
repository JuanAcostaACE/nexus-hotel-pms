package com.nexus.hotelpms.aplicacion.puertos;

import com.nexus.hotelpms.dominio.modelo.CatalogoItem;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

/** Puerto de persistencia de CatalogoItems (Spring Data genera la implementación). */
public interface ICatalogoItemRepository extends MongoRepository<CatalogoItem, String> {

    List<CatalogoItem> findBySedeId(String sedeId);    List<CatalogoItem> findBySedeIdAndActivoTrue(String sedeId);
}
