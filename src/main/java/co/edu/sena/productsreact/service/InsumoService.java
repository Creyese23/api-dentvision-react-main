package co.edu.sena.productsreact.service;

import co.edu.sena.productsreact.dto.insumo.InsumoRequest;
import co.edu.sena.productsreact.dto.insumo.InsumoResponse;
import co.edu.sena.productsreact.dto.insumo.MovimientoRequest;
import co.edu.sena.productsreact.entity.Empleado;
import co.edu.sena.productsreact.entity.Insumo;
import co.edu.sena.productsreact.entity.MovimientoInventario;
import co.edu.sena.productsreact.exception.ResourceNotFoundException;
import co.edu.sena.productsreact.repository.EmpleadoRepository;
import co.edu.sena.productsreact.repository.InsumoRepository;
import co.edu.sena.productsreact.repository.MovimientoInventarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@org.springframework.transaction.annotation.Transactional(readOnly = true)
public class InsumoService {

    private final InsumoRepository insumoRepository;
    private final MovimientoInventarioRepository movimientoRepository;
    private final EmpleadoRepository empleadoRepository;

    public List<InsumoResponse> listarActivos() {
        return insumoRepository.findByEstadoTrueAndFechaEliminacionIsNull()
                .stream().map(InsumoResponse::from).toList();
    }

    public List<InsumoResponse> listarBajoStock() {
        return insumoRepository.findBajoStock().stream().map(InsumoResponse::from).toList();
    }

    public InsumoResponse obtener(Long id) {
        return InsumoResponse.from(getOrThrow(id));
    }

    @Transactional
    public InsumoResponse crear(InsumoRequest req) {
        Insumo i = Insumo.builder()
                .nombre(req.nombre()).descripcion(req.descripcion())
                .unidadMedida(req.unidadMedida())
                .stockActual(req.stockActual() != null ? req.stockActual() : 0)
                .stockMinimo(req.stockMinimo() != null ? req.stockMinimo() : 0)
                .precioUnitario(req.precioUnitario()).estado(true)
                .build();
        return InsumoResponse.from(insumoRepository.save(i));
    }

    @Transactional
    public InsumoResponse actualizar(Long id, InsumoRequest req) {
        Insumo i = getOrThrow(id);
        i.setNombre(req.nombre());
        i.setDescripcion(req.descripcion());
        i.setUnidadMedida(req.unidadMedida());
        if (req.stockActual() != null) i.setStockActual(req.stockActual());
        i.setStockMinimo(req.stockMinimo() != null ? req.stockMinimo() : i.getStockMinimo());
        i.setPrecioUnitario(req.precioUnitario());
        return InsumoResponse.from(insumoRepository.save(i));
    }

    @Transactional
    public InsumoResponse registrarMovimiento(MovimientoRequest req) {
        Insumo insumo = getOrThrow(req.idInsumo());
        Empleado empleado = null;
        if (req.idEmpleado() != null) {
            empleado = empleadoRepository.findById(req.idEmpleado())
                    .orElseThrow(() -> new ResourceNotFoundException("Empleado " + req.idEmpleado() + " no encontrado"));
        }

        String tipo = req.tipoMovimiento().toUpperCase();
        if ("ENTRADA".equals(tipo)) {
            insumo.setStockActual(insumo.getStockActual() + req.cantidad());
        } else if ("SALIDA".equals(tipo)) {
            if (insumo.getStockActual() < req.cantidad()) {
                throw new IllegalArgumentException("Stock insuficiente. Disponible: " + insumo.getStockActual());
            }
            insumo.setStockActual(insumo.getStockActual() - req.cantidad());
        }

        MovimientoInventario mov = MovimientoInventario.builder()
                .insumo(insumo).empleado(empleado)
                .cantidad(req.cantidad()).tipoMovimiento(tipo)
                .build();
        movimientoRepository.save(mov);
        return InsumoResponse.from(insumoRepository.save(insumo));
    }

    @Transactional
    public void eliminar(Long id) {
        Insumo i = getOrThrow(id);
        i.setEstado(false);
        i.setFechaEliminacion(LocalDateTime.now());
        insumoRepository.save(i);
    }

    private Insumo getOrThrow(Long id) {
        return insumoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Insumo " + id + " no encontrado"));
    }
}
