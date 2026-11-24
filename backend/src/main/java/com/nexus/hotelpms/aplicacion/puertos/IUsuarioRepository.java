package com.nexus.hotelpms.aplicacion.puertos;

import com.nexus.hotelpms.dominio.modelo.Usuario;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

/** Puerto de persistencia de Usuarios (Spring Data genera la implementación). */
public interface IUsuarioRepository extends MongoRepository<Usuario, String> {
    Optional<Usuario> findByEmail(String email);
}
