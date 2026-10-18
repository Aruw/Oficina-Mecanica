package com.aruw.oficina.application.cliente;

import com.aruw.oficina.application.shared.RecursoNaoEncontradoException;
import com.aruw.oficina.domain.cliente.Cliente;
import com.aruw.oficina.domain.cliente.ClienteRepository;
import com.aruw.oficina.domain.ordemservico.OrdemServicoRepository;
import com.aruw.oficina.domain.shared.Documento;
import com.aruw.oficina.domain.shared.DomainException;
import com.aruw.oficina.domain.veiculo.VeiculoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class ClienteService {

    private final ClienteRepository clientes;
    private final VeiculoRepository veiculos;
    private final OrdemServicoRepository ordens;

    public ClienteService(ClienteRepository clientes, VeiculoRepository veiculos, OrdemServicoRepository ordens) {
        this.clientes = clientes;
        this.veiculos = veiculos;
        this.ordens = ordens;
    }

    @Transactional
    public Cliente criar(ClienteCommand cmd) {
        Documento documento = Documento.of(cmd.documento());
        if (clientes.existsByDocumentoValor(documento.getValor())) {
            throw new DomainException("Já existe cliente cadastrado com o documento " + documento.formatado());
        }
        return clientes.save(new Cliente(cmd.nome(), documento, cmd.email(), cmd.telefone()));
    }

    @Transactional
    public Cliente atualizar(UUID id, ClienteCommand cmd) {
        Cliente cliente = obter(id);
        cliente.atualizar(cmd.nome(), cmd.email(), cmd.telefone());
        return clientes.save(cliente);
    }

    @Transactional(readOnly = true)
    public Cliente buscar(UUID id) {
        return obter(id);
    }

    private Cliente obter(UUID id) {
        return clientes.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Cliente", id));
    }

    @Transactional(readOnly = true)
    public Cliente buscarPorDocumento(String documento) {
        String valor = Documento.of(documento).getValor();
        return clientes.findByDocumentoValor(valor)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente", documento));
    }

    @Transactional(readOnly = true)
    public List<Cliente> listar() {
        return clientes.findAll();
    }

    @Transactional
    public void remover(UUID id) {
        Cliente cliente = obter(id);
        if (veiculos.existsByClienteId(id) || ordens.existsByClienteId(id)) {
            throw new DomainException("Cliente possui veículos ou ordens de serviço vinculados e não pode ser removido");
        }
        clientes.delete(cliente);
    }
}
