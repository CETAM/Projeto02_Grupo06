"""
Conexão e queries auxiliares no MySQL (grupo06_db).

Usado principalmente pelos testes de Banco de Dados (TC-01 a TC-05), e por
outros testes que precisam conferir o estado real das tabelas depois de uma
ação feita via HTTP/Selenium (ex.: estoque devolvido, item gravado, etc.).
"""

import mysql.connector

from config import DB_HOST, DB_PORT, DB_NAME, DB_USER, DB_PASSWORD


def get_connection():
    """
    Abre uma nova conexão com o grupo06_db.

    autocommit=True é importante aqui: por padrão o mysql-connector-python
    abre a conexão com autocommit=False, e a primeira consulta já inicia uma
    transação (REPEATABLE READ). Sem isso, uma consulta feita logo após um
    INSERT da aplicação (em outra conexão) pode não enxergar o commit
    recém-feito, fazendo os testes acharem que o registro não foi criado.
    """
    return mysql.connector.connect(
        host=DB_HOST,
        port=DB_PORT,
        database=DB_NAME,
        user=DB_USER,
        password=DB_PASSWORD,
        autocommit=True,
    )


def fetch_all(conn, sql, params=None):
    cursor = conn.cursor(dictionary=True)
    cursor.execute(sql, params or ())
    rows = cursor.fetchall()
    cursor.close()
    return rows


def fetch_one(conn, sql, params=None):
    rows = fetch_all(conn, sql, params)
    return rows[0] if rows else None


def buscar_id_cliente_por_email(conn, email):
    row = fetch_one(conn, "SELECT id FROM clientes WHERE email = %s", (email,))
    return row["id"] if row else None


def buscar_id_produto_por_nome(conn, nome):
    row = fetch_one(conn, "SELECT id FROM produtos WHERE nome = %s", (nome,))
    return row["id"] if row else None


def buscar_id_categoria_por_nome(conn, nome):
    row = fetch_one(conn, "SELECT id FROM categorias WHERE nome = %s", (nome,))
    return row["id"] if row else None


def buscar_ultimo_pedido_do_cliente(conn, cliente_id):
    return fetch_one(
        conn,
        """
        SELECT * FROM pedidos
        WHERE cliente_id = %s
        ORDER BY data_pedido DESC, id DESC
        LIMIT 1
        """,
        (cliente_id,),
    )


def buscar_estoque_produto(conn, produto_id):
    row = fetch_one(conn, "SELECT quantidade_estoque FROM produtos WHERE id = %s", (produto_id,))
    return row["quantidade_estoque"] if row else None


def buscar_itens_do_pedido(conn, pedido_id):
    return fetch_all(conn, "SELECT * FROM itens_pedido WHERE pedido_id = %s", (pedido_id,))


# ----------------------------------------------------------------------
# General query log (usado para provar, com evidência real, quantas e quais
# instruções SQL a aplicação manda pro MySQL numa única operação — TC-04)
# ----------------------------------------------------------------------
def capturar_configuracao_log(conn):
    """Lê o estado atual do general_log, para restaurar depois do teste."""
    geral = fetch_one(conn, "SHOW VARIABLES LIKE 'general_log'")["Value"]
    saida = fetch_one(conn, "SHOW VARIABLES LIKE 'log_output'")["Value"]
    return geral, saida


def habilitar_general_log(conn):
    """
    Liga o general_log do MySQL, gravando numa tabela (mysql.general_log)
    pra dar pra consultar via SQL. Precisa de privilégio SUPER/
    SYSTEM_VARIABLES_ADMIN — o usuário 'root' local normalmente tem.
    """
    cursor = conn.cursor()
    cursor.execute("SET GLOBAL log_output = 'TABLE'")
    cursor.execute("SET GLOBAL general_log = 'ON'")
    cursor.close()


def restaurar_configuracao_log(conn, geral_original, saida_original):
    cursor = conn.cursor()
    cursor.execute("SET GLOBAL log_output = %s", (saida_original,))
    cursor.execute("SET GLOBAL general_log = %s", (geral_original,))
    cursor.close()


def momento_atual_do_servidor(conn):
    """Horário do próprio servidor MySQL (evita desvio de relógio com a máquina local)."""
    return fetch_one(conn, "SELECT NOW(6) AS agora")["agora"]


def buscar_instrucoes_sobre_tabela(conn, tabela, desde, contendo=None):
    """
    Lê o mysql.general_log em busca de instruções (INSERT/UPDATE/...) que
    mencionam `tabela`, a partir do horário `desde`. `contendo`, se passado,
    filtra ainda mais (ex.: o id do pedido criado no teste).
    """
    sql = """
        SELECT event_time, argument
        FROM mysql.general_log
        WHERE event_time >= %s
          AND command_type = 'Query'
          AND argument LIKE %s
        ORDER BY event_time
    """
    linhas = fetch_all(conn, sql, (desde, f"%{tabela}%"))

    # A coluna 'argument' do mysql.general_log é MEDIUMBLOB — o conector
    # devolve bytes, não str. Decodifica pra facilitar comparação de texto.
    for linha in linhas:
        if isinstance(linha["argument"], (bytes, bytearray)):
            linha["argument"] = linha["argument"].decode("utf-8", errors="replace")

    if contendo:
        linhas = [linha for linha in linhas if contendo in linha["argument"]]
    return linhas
