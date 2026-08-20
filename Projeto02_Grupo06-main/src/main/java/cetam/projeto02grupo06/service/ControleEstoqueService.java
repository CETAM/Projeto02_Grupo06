package cetam.projeto02grupo06.service;
import cetam.projeto02grupo06.model.ControleEstoque;
import cetam.projeto02grupo06.model.Produto;
import cetam.projeto02grupo06.repository.ControleEstoqueRepository;
import cetam.projeto02grupo06.repository.ProdutoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
public class ControleEstoqueService {

    private final ControleEstoqueRepository controleRepository;
    private final ProdutoRepository produtoRepository;


    public ControleEstoqueService(ControleEstoqueRepository controleRepository,
                                  ProdutoRepository produtoRepository) {
        this.controleRepository = controleRepository;
        this.produtoRepository = produtoRepository;
    }


    public List<ControleEstoque> listarTodos() {
        return controleRepository.findAllByOrderByDataMovimentoDesc();
    }


    public ControleEstoque buscarPorId(Integer id) {
        validarId(id);
        return controleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Controle de estoque não encontrado"));
    }


    public List<ControleEstoque> buscarPorProduto(Integer produtoId) {
        validarId(produtoId);
        validarProdutoExiste(produtoId);
        return controleRepository.findByProdutoIdOrderByDataMovimentoDesc(produtoId);
    }


    public List<ControleEstoque> buscarPorTipo(String tipoMovimento) {
        validarTipoMovimento(tipoMovimento);
        return controleRepository.findByTipoMovimentoOrderByDataMovimentoDesc(tipoMovimento);
    }


    public List<ControleEstoque> buscarPorPeriodo(LocalDateTime dataInicio, LocalDateTime dataFim) {
        if (dataInicio == null || dataFim == null) {
            throw new IllegalArgumentException("Datas são obrigatórias");
        }
        if (dataInicio.isAfter(dataFim)) {
            throw new IllegalArgumentException("Data de início não pode ser após a data de fim");
        }
        return controleRepository.findByDataMovimentoBetweenOrderByDataMovimentoDesc(dataInicio, dataFim);
    }


    public List<ControleEstoque> buscarPorProdutoETipo(Integer produtoId, String tipoMovimento) {
        validarId(produtoId);
        validarTipoMovimento(tipoMovimento);
        return controleRepository.findByProdutoIdAndTipoMovimentoOrderByDataMovimentoDesc(produtoId, tipoMovimento);
    }


    public List<ControleEstoque> buscarMovimentacoesRecentes(Integer produtoId, LocalDateTime dataLimite) {
        validarId(produtoId);
        if (dataLimite == null) {
            throw new IllegalArgumentException("Data limite é obrigatória");
        }
        return controleRepository.findMovimentacoesRecentes(produtoId, dataLimite);
    }


    @Transactional
    public void registrarEntrada(Integer produtoId, Integer quantidade, String observacoes) {
        validarId(produtoId);
        validarQuantidade(quantidade);

        Produto produto = buscarProduto(produtoId);
        Integer quantidadeAnterior = produto.getQuantidadeEstoque();
        Integer quantidadeNova = quantidadeAnterior + quantidade;

        registrarControle(produto, quantidadeAnterior, quantidadeNova, "ENTRADA", observacoes);
        atualizarQuantidadeProduto(produto, quantidadeNova);
    }


    @Transactional
    public void registrarSaida(Integer produtoId, Integer quantidade, String observacoes) {
        validarId(produtoId);
        validarQuantidade(quantidade);

        Produto produto = buscarProduto(produtoId);

        if (produto.getQuantidadeEstoque() < quantidade) {
            throw new IllegalArgumentException(
                    String.format("Estoque insuficiente. Disponível: %d, Solicitado: %d",
                            produto.getQuantidadeEstoque(), quantidade)
            );
        }

        Integer quantidadeAnterior = produto.getQuantidadeEstoque();
        Integer quantidadeNova = quantidadeAnterior - quantidade;

        registrarControle(produto, quantidadeAnterior, quantidadeNova, "SAÍDA", observacoes);
        atualizarQuantidadeProduto(produto, quantidadeNova);
    }


    @Transactional
    public void registrarAjuste(Integer produtoId, Integer novaQuantidade, String observacoes) {
        validarId(produtoId);

        if (novaQuantidade == null || novaQuantidade < 0) {
            throw new IllegalArgumentException("Quantidade não pode ser negativa");
        }

        Produto produto = buscarProduto(produtoId);
        Integer quantidadeAnterior = produto.getQuantidadeEstoque();

        registrarControle(produto, quantidadeAnterior, novaQuantidade, "AJUSTE", observacoes);
        atualizarQuantidadeProduto(produto, novaQuantidade);
    }


    public void salvar(ControleEstoque controle) {
        if (controle == null) {
            throw new IllegalArgumentException("Controle não pode ser nulo");
        }
        if (controle.getProduto() == null) {
            throw new IllegalArgumentException("Produto é obrigatório");
        }
        if (controle.getQuantidadeAnterior() == null || controle.getQuantidadeAnterior() < 0) {
            throw new IllegalArgumentException("Quantidade anterior inválida");
        }
        if (controle.getQuantidadeNova() == null || controle.getQuantidadeNova() < 0) {
            throw new IllegalArgumentException("Quantidade nova inválida");
        }
        validarTipoMovimento(controle.getTipoMovimento());

        controleRepository.save(controle);
    }


    @Transactional
    public void excluir(Integer id) {
        validarId(id);
        ControleEstoque controle = buscarPorId(id);
        controleRepository.delete(controle);
    }


    public Long obterTotalPorTipo(String tipoMovimento) {
        validarTipoMovimento(tipoMovimento);
        return controleRepository.countByTipoMovimento(tipoMovimento);
    }


    public Long obterTotalPorProduto(Integer produtoId) {
        validarId(produtoId);
        return controleRepository.countByProdutoId(produtoId);
    }


    public Long obterTotalMovimentacoes() {
        return controleRepository.count();
    }


    private Produto buscarProduto(Integer produtoId) {
        return produtoRepository.findById(produtoId)
                .orElseThrow(() -> new IllegalArgumentException("Produto não encontrado"));
    }


    private void registrarControle(Produto produto, Integer quantidadeAnterior, Integer quantidadeNova,
                                   String tipoMovimento, String observacoes) {
        ControleEstoque controle = new ControleEstoque(produto, quantidadeAnterior, quantidadeNova,
                tipoMovimento, observacoes);
        controleRepository.save(controle);
    }


    private void atualizarQuantidadeProduto(Produto produto, Integer novaQuantidade) {
        produto.setQuantidadeEstoque(novaQuantidade);
        produtoRepository.save(produto);
    }


    private void validarId(Integer id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("ID inválido");
        }
    }


    private void validarQuantidade(Integer quantidade) {
        if (quantidade == null || quantidade <= 0) {
            throw new IllegalArgumentException("Quantidade deve ser maior que zero");
        }
    }


    private void validarTipoMovimento(String tipoMovimento) {
        if (tipoMovimento == null || tipoMovimento.trim().isEmpty()) {
            throw new IllegalArgumentException("Tipo de movimento é obrigatório");
        }
        if (!tipoMovimento.equals("ENTRADA") && !tipoMovimento.equals("SAÍDA") && !tipoMovimento.equals("AJUSTE")) {
            throw new IllegalArgumentException("Tipo de movimento inválido. Use: ENTRADA, SAÍDA ou AJUSTE");
        }
    }


    private void validarProdutoExiste(Integer produtoId) {
        if (!produtoRepository.existsById(produtoId)) {
            throw new IllegalArgumentException("Produto não encontrado");
        }
    }
}

