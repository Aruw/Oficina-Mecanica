package com.aruw.oficina.application.veiculo;

import com.aruw.oficina.application.shared.RecursoNaoEncontradoException;
import com.aruw.oficina.domain.cliente.ClienteRepository;
import com.aruw.oficina.domain.ordemservico.OrdemServicoRepository;
import com.aruw.oficina.domain.shared.DomainException;
import com.aruw.oficina.domain.shared.Placa;
import com.aruw.oficina.domain.veiculo.Veiculo;
import com.aruw.oficina.domain.veiculo.VeiculoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class VeiculoService {

    private final VeiculoRepository veiculos;
    private final ClienteRepository clientes;
    private final OrdemServicoRepository ordens;

    public VeiculoService(VeiculoRepository veiculos, ClienteRepository clientes, OrdemServicoRepository ordens) {
        this.veiculos = veiculos;
        this.clientes = clientes;
        this.ordens = ordens;
    }

    @Transactional
    public Veiculo criar(VeiculoCommand cmd) {
        Placa placa = Placa.of(cmd.placa());
        if (veiculos.existsByPlacaValor(placa.getValor())) {
            throw new DomainException("Já existe veículo cadastrado com a placa " + placa);
        }
        exigirCliente(cmd.clienteId());
        return veiculos.save(new Veiculo(placa, cmd.marca(), cmd.modelo(), cmd.ano(), cmd.clienteId()));
    }

    @Transactional
    public Veiculo atualizar(UUID id, VeiculoCommand cmd) {
        Veiculo veiculo = obter(id);
        exigirCliente(cmd.clienteId());
        veiculo.atualizar(cmd.marca(), cmd.modelo(), cmd.ano(), cmd.clienteId());
        return veiculos.save(veiculo);
    }

    @Transactional(readOnly = true)
    public Veiculo buscar(UUID id) {
        return obter(id);
    }

    private Veiculo obter(UUID id) {
        return veiculos.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Veículo", id));
    }

    @Transactional(readOnly = true)
    public List<Veiculo> listar(UUID clienteId) {
        return clienteId == null ? veiculos.findAll() : veiculos.findByClienteId(clienteId);
    }

    @Transactional
    public void remover(UUID id) {
        Veiculo veiculo = obter(id);
        if (ordens.existsByVeiculoId(id)) {
            throw new DomainException("Veículo possui ordens de serviço vinculadas e não pode ser removido");
        }
        veiculos.delete(veiculo);
    }

    private void exigirCliente(UUID clienteId) {
        if (clienteId == null || clientes.findById(clienteId).isEmpty()) {
            throw new RecursoNaoEncontradoException("Cliente", clienteId);
        }
    }
}
