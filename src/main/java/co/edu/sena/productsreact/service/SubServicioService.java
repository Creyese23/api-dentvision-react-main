package co.edu.sena.productsreact.service;

import co.edu.sena.productsreact.dto.servicio.SubServicioRequest;
import co.edu.sena.productsreact.dto.servicio.SubServicioResponse;
import co.edu.sena.productsreact.entity.Servicio;
import co.edu.sena.productsreact.entity.SubServicio;
import co.edu.sena.productsreact.exception.ResourceNotFoundException;
import co.edu.sena.productsreact.repository.ServicioRepository;
import co.edu.sena.productsreact.repository.SubServicioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SubServicioService {

    private final SubServicioRepository subServicioRepository;
    private final ServicioRepository servicioRepository;

    public List<SubServicioResponse> listarPorServicio(Long servicioId) {
        return subServicioRepository.findByServicioIdAndEstadoTrue(servicioId)
                .stream().map(SubServicioResponse::from).toList();
    }

    @Transactional
    public SubServicioResponse crear(Long servicioId, SubServicioRequest req) {
        Servicio servicio = servicioRepository.findById(servicioId)
                .orElseThrow(() -> new ResourceNotFoundException("Servicio " + servicioId + " no encontrado"));
        SubServicio sub = SubServicio.builder()
                .servicio(servicio)
                .nombre(req.nombre())
                .descripcion(req.descripcion())
                .precio(req.precio())
                .duracionMinutos(req.duracionMinutos())
                .estado(true)
                .build();
        return SubServicioResponse.from(subServicioRepository.save(sub));
    }

    @Transactional
    public SubServicioResponse actualizar(Long id, SubServicioRequest req) {
        SubServicio sub = subServicioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sub-servicio " + id + " no encontrado"));
        sub.setNombre(req.nombre());
        sub.setDescripcion(req.descripcion());
        sub.setPrecio(req.precio());
        sub.setDuracionMinutos(req.duracionMinutos());
        return SubServicioResponse.from(subServicioRepository.save(sub));
    }

    @Transactional
    public void eliminar(Long id) {
        SubServicio sub = subServicioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sub-servicio " + id + " no encontrado"));
        sub.setEstado(false);
        subServicioRepository.save(sub);
    }
}
