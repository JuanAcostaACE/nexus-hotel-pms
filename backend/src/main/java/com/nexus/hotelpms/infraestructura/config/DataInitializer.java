package com.nexus.hotelpms.infraestructura.config;

import com.nexus.hotelpms.aplicacion.puertos.*;
import com.nexus.hotelpms.dominio.modelo.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Datos de arranque para poder probar en Postman (solo si la base está vacía y hotel.seed-habilitado=true).
 * Usuarios: admin@hotel.com / Admin123*  y  recepcionista@hotel.com / Recep123*  (sede Norte).
 */
@Component
public class DataInitializer implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final ISedeRepository sedes;
    private final IUsuarioRepository usuarios;
    private final IHabitacionRepository habitaciones;
    private final ICatalogoItemRepository catalogo;
    private final PasswordEncoder encoder;
    private final HotelProperties props;

    public DataInitializer(ISedeRepository sedes, IUsuarioRepository usuarios, IHabitacionRepository habitaciones,
                           ICatalogoItemRepository catalogo, PasswordEncoder encoder, HotelProperties props) {
        this.sedes = sedes;
        this.usuarios = usuarios;
        this.habitaciones = habitaciones;
        this.catalogo = catalogo;
        this.encoder = encoder;
        this.props = props;
    }

    @Override
    public void run(String... args) {
        if (!props.seedHabilitado() || sedes.count() > 0) {
            return;
        }
        Sede norte = sedes.save(new Sede("Sede Norte", "Calle 100 # 15-20, Bogotá"));
        Sede sur = sedes.save(new Sede("Sede Sur", "Carrera 30 # 5-10, Bogotá"));

        usuarios.save(new Administrador("Administrador General", "admin@hotel.com", encoder.encode(props.seedAdminPassword())));
        usuarios.save(new Recepcionista("Recepción Norte", "recepcionista@hotel.com", encoder.encode(props.seedRecepPassword()), norte.getId()));
        usuarios.save(new Recepcionista("Recepción Sur", "recepcion.sur@hotel.com", encoder.encode(props.seedRecepPassword()), sur.getId()));

        for (Sede s : new Sede[]{norte, sur}) {
            habitaciones.guardar(new Habitacion(s.getId(), "101", "Simple", 1, new BigDecimal("120000")));
            habitaciones.guardar(new Habitacion(s.getId(), "102", "Doble", 2, new BigDecimal("180000")));
            habitaciones.guardar(new Habitacion(s.getId(), "201", "Familiar", 4, new BigDecimal("260000")));
            habitaciones.guardar(new Habitacion(s.getId(), "205", "Doble", 2, new BigDecimal("190000")));
            catalogo.save(new CatalogoItem(s.getId(), "Agua mineral 500ml", new BigDecimal("5000"), CategoriaConsumo.Minibar));
            catalogo.save(new CatalogoItem(s.getId(), "Almuerzo ejecutivo", new BigDecimal("35000"), CategoriaConsumo.Restaurante));
            CatalogoItem vino = new CatalogoItem(s.getId(), "Botella de vino tinto", new BigDecimal("90000"), CategoriaConsumo.Minibar);
            vino.inactivar(); // ítem inactivo para probar HU-09
            catalogo.save(vino);
        }
        log.info("Datos de ejemplo creados (2 sedes, 3 usuarios, 8 habitaciones, catálogo).");
    }
}
