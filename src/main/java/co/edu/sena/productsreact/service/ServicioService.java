package co.edu.sena.productsreact.service;

import co.edu.sena.productsreact.dto.servicio.ServicioRequest;
import co.edu.sena.productsreact.dto.servicio.ServicioResponse;
import co.edu.sena.productsreact.entity.Servicio;
import co.edu.sena.productsreact.exception.DuplicateResourceException;
import co.edu.sena.productsreact.exception.ResourceNotFoundException;
import co.edu.sena.productsreact.repository.ServicioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ServicioService {

    private final ServicioRepository servicioRepository;

    public List<ServicioResponse> listarActivos() {
        return servicioRepository.findByEstadoTrue().stream()
                .map(ServicioResponse::from).toList();
    }

    public ServicioResponse obtener(Long id) {
        return ServicioResponse.from(getOrThrow(id));
    }

    @Transactional
    public ServicioResponse crear(ServicioRequest req) {
        if (servicioRepository.existsByNombre(req.nombre())) {
            throw new DuplicateResourceException("Ya existe el servicio '" + req.nombre() + "'");
        }
        Servicio s = Servicio.builder()
                .nombre(req.nombre()).descripcion(req.descripcion())
                .precio(req.precio()).duracionMinutos(req.duracionMinutos())
                .estado(true).build();
        return ServicioResponse.from(servicioRepository.save(s));
    }

    @Transactional
    public ServicioResponse actualizar(Long id, ServicioRequest req) {
        Servicio s = getOrThrow(id);
        s.setNombre(req.nombre());
        s.setDescripcion(req.descripcion());
        s.setPrecio(req.precio());
        s.setDuracionMinutos(req.duracionMinutos());
        return ServicioResponse.from(servicioRepository.save(s));
    }

    @Transactional
    public void desactivar(Long id) {
        Servicio s = getOrThrow(id);
        s.setEstado(false);
        servicioRepository.save(s);
    }

    public Servicio getOrThrow(Long id) {
        return servicioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Servicio " + id + " no encontrado"));
    }
}
