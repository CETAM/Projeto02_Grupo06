/*CREATE DATABASE IF NOT EXISTS sistema_controle_pedidos;
-- Conectar ao banco de dados
 USE sistema_controle_pedidos;

-- Tabela de Categorias
CREATE TABLE categorias (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nome VARCHAR(100) NOT NULL UNIQUE,
    descricao TEXT,
    data_criacao date
);

-- Tabela de Produtos
CREATE TABLE produtos (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nome VARCHAR(150) NOT NULL,
    descricao TEXT,
    preco DECIMAL(10, 2) NOT NULL,
    quantidade_estoque INTEGER NOT NULL DEFAULT 0,
    categoria_id INTEGER NOT NULL,
    data_criacao date,
    FOREIGN KEY (categoria_id) REFERENCES categorias(id)
);


-- Tabela de Clientes
CREATE TABLE clientes (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nome VARCHAR(150) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    telefone VARCHAR(20),
    endereco TEXT,
    cidade VARCHAR(100),
    estado VARCHAR(2),
    cep VARCHAR(10),
    data_criacao date,
    ativo BOOLEAN DEFAULT TRUE
);

-- Tabela de Pedidos (CORRIGIDO)
CREATE TABLE pedidos (
    id INT AUTO_INCREMENT PRIMARY KEY,
    cliente_id INTEGER NOT NULL,
    data_pedido TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    data_entrega TIMESTAMP NULL,
    status VARCHAR(50) DEFAULT 'Pendente',
    valor_total DECIMAL(10, 2) DEFAULT 0,
    observacoes TEXT,
    FOREIGN KEY (cliente_id) REFERENCES clientes(id)
);

 -- Trigger para calcular data_entrega automaticamente
DELIMITER $$

DELIMITER $$
CREATE TRIGGER trg_set_data_entrega
BEFORE INSERT ON pedidos
FOR EACH ROW
BEGIN
  IF NEW.data_entrega IS NULL THEN
    SET NEW.data_entrega = DATE_ADD(NEW.data_pedido, INTERVAL (5 + FLOOR(RAND() * 3)) DAY);
    SET NEW.data_entrega = DATE_ADD(NEW.data_entrega, INTERVAL FLOOR(RAND() * 24) HOUR);
    SET NEW.data_entrega = DATE_ADD(NEW.data_entrega, INTERVAL FLOOR(RAND() * 60) MINUTE);
  END IF;
END$$
DELIMITER ;
 -- Tabela de Itens do Pedido
CREATE TABLE itens_pedido (
    id INT AUTO_INCREMENT PRIMARY KEY,
    pedido_id INTEGER NOT NULL,
    produto_id INTEGER NOT NULL,
    quantidade INTEGER NOT NULL,
    preco_unitario DECIMAL(10, 2) NOT NULL,
    subtotal DECIMAL(10, 2) NOT NULL,
    FOREIGN KEY (pedido_id) REFERENCES pedidos(id) ON DELETE CASCADE,
    FOREIGN KEY (produto_id) REFERENCES produtos(id)
);

-- Tabela de Controle de Estoque
CREATE TABLE controle_estoque (
    id INT AUTO_INCREMENT PRIMARY KEY,
    produto_id INTEGER NOT NULL,
    quantidade_anterior INTEGER,
    quantidade_nova INTEGER,
    tipo_movimento VARCHAR(50),
    data_movimento datetime DEFAULT CURRENT_TIMESTAMP NOT NULL,
    observacoes TEXT,
    FOREIGN KEY (produto_id) REFERENCES produtos(id)
);



 -- Inserir dados de exemplo
INSERT INTO categorias (nome, descricao, data_criacao) VALUES 
('Eletrônicos', 'Produtos eletrônicos diversos','2026-01-10'), 
('Roupas', 'Vestuário em geral','2026-01-15'), 
('Alimentos', 'Produtos alimentícios','2026-01-20');

-- ========================================
-- INSERIR PRODUTOS
-- ========================================

INSERT INTO produtos (nome, descricao, preco, quantidade_estoque, categoria_id, data_criacao) VALUES 
('Notebook Dell', 'Notebook Intel Core i5', 2500.00, 10, 1, '2026-02-11'),
('Smartphone Samsung', 'Smartphone Android 128GB', 1200.00, 15, 1, '2026-04-16'),
('Camiseta Básica', 'Camiseta 100% algodão', 49.90, 50, 2, '2026-04-21'),
('Calça Jeans', 'Calça jeans azul', 89.90, 30, 2, '2026-05-20'),
('Arroz 5kg', 'Arroz integral 5kg', 25.00, 100, 3, '2026-03-15');

-- ========================================
-- INSERIR CLIENTES
-- ========================================

INSERT INTO clientes (nome, email, telefone, endereco, cidade, estado, cep, data_criacao) VALUES 
('João Silva', 'joao@email.com', '(11)988453627', 'Rua A, 123', 'São Paulo', 'SP', '01234-567', '2026-01-12'),
('Maria Santos', 'maria@email.com', '(92)986172564', 'Avenida Brasil, 45', 'Manaus', 'AM', '69099-120', '2026-05-17'),
('Pedro Costa', 'pedro@email.com', '(11)988775644', 'Rua D.Pedro l, 789', 'São Paulo', 'SP', '30100-000', '2026-03-11');

-- ========================================
-- INSERIR MOVIMENTAÇÕES DE ESTOQUE
-- ========================================

INSERT INTO controle_estoque (produto_id, quantidade_anterior, quantidade_nova, tipo_movimento, data_movimento, observacoes) VALUES
(1, 0, 10, 'Entrada', '2026-01-15 09:30:00',  'Entrada inicial de notebooks'),
(2, 0, 15, 'Entrada', '2026-01-15 14:20:00' ,'Entrada inicial de smartphones'),
(3, 0, 50, 'Entrada','2026-01-16 08:15:00', 'Entrada inicial de camisetas'),
(4, 0, 30, 'Entrada','2026-01-22 11:45:00', 'Entrada inicial de calças'),
(5, 0, 100, 'Entrada','2026-01-16 16:30:00', 'Entrada inicial de arroz'),
(1, 10, 9, 'Saída','2026-01-27 16:30:00', 'Venda - Pedido 1 - João Silva'),
(3, 50, 49, 'Saída','2026-01-31 11:45:00', 'Venda - Pedido 1 - João Silva'),
(2, 15, 14, 'Saída','2026-03-30 08:15:00', 'Venda - Pedido 2 - Maria Santos'),
(4, 30, 29, 'Saída','2026-04-25 14:20:00', 'Venda - Pedido 2 - Maria Santos'),
(3, 49, 47, 'Saída', '2026-06-15 09:30:00','Venda - Pedido 3 - Pedro Costa (2 unidades)'),
(4, 29, 28, 'Saída', '2026-02-16 08:15:00','Venda - Pedido 3 - Pedro Costa'),
(5, 100, 99, 'Saída','2026-06-16 16:30:00', 'Venda - Pedido 4 - João Silva'),
(2, 14, 15, 'Ajuste', '2026-07-17 11:45:00', 'Devolução de smartphone - cliente insatisfeito');

-- ========================================
-- INSERIR PEDIDOS
-- ========================================
INSERT INTO pedidos (cliente_id, data_pedido, status, valor_total, observacoes) VALUES 
(1, '2026-01-27', 'Entregue', 2549.90, 'Pedido entregue no prazo'), 
(1, '2026-01-31', 'Processando', 1200.00, 'Aguardando processamento'), 
(1, '2026-03-30', 'Pendente', 0.00, 'Aguardando confirmação de pagamento'),
(2, '2026-04-25', 'Entregue', 89.90, 'Entregue com sucesso'), 
(2, '2026-06-15', 'Entregue', 2749.00, 'Cliente satisfeito'), 
(2, '2026-02-16', 'Processando', 1500.00, 'Em separação no almoxarifado'),
(3, '2026-06-16', 'Entregue', 139.80, 'Entregue conforme solicitado'), 
(3, '2026-02-17', 'Pendente', 2525.00, 'Cliente solicitou adiamento'), 
(3, '2026-02-10', 'Cancelado', 0.00, 'Cancelado por solicitação do cliente');



-- ========================================
-- INSERIR ITENS DOS PEDIDOS
-- ========================================

INSERT INTO itens_pedido (pedido_id, produto_id, quantidade, preco_unitario, subtotal) VALUES
(1, 1, 1, 2500.00, 2500.00),
(1, 3, 1, 49.90, 49.90),
(2, 2, 1, 1200.00, 1200.00),
(2, 4, 5, 49.80,249.00 ),
(3, 3, 2, 49.90, 99.80),
(3, 4, 1, 40.00, 40.00),
(4, 5, 1, 25.00, 25.00);

INSERT INTO itens_pedido (pedido_id, produto_id, quantidade, preco_unitario, subtotal) VALUES
(2, 2, 1, 1200.00, 1200.00),
(2, 4, 1, 49.80, 49.80);

INSERT INTO itens_pedido (pedido_id, produto_id, quantidade, preco_unitario, subtotal) VALUES
(3, 3, 2, 49.90, 99.80),
(3, 4, 1, 40.00, 40.00);

INSERT INTO itens_pedido (pedido_id, produto_id, quantidade, preco_unitario, subtotal) VALUES
(4, 5, 1, 25.00, 25.00);*/


select * from ;












