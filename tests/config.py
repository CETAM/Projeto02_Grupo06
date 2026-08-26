"""
Configurações centrais da suíte de testes.

Tudo aqui pode ser sobrescrito por variável de ambiente, então dá pra rodar
em outra máquina/porta sem editar o código. Ex.:

    set BASE_URL=http://localhost:8080
    set DB_PASSWORD=12345678
    pytest
"""

import os

# ----------------------------------------------------------------------
# Aplicação (Spring Boot)
# ----------------------------------------------------------------------
BASE_URL = os.getenv("BASE_URL", "http://localhost:8080")

# ----------------------------------------------------------------------
# Banco de dados (MySQL) — usados pelos testes de Banco de Dados (TC-01 a TC-05)
# e por qualquer teste que precise consultar o estado real das tabelas.
#
# ATENÇÃO: o application.properties do projeto usa senha "12345678" (8 dígitos).
# Se a sua senha real for diferente, exporte DB_PASSWORD antes de rodar os testes.
# ----------------------------------------------------------------------
DB_HOST = os.getenv("DB_HOST", "localhost")
DB_PORT = int(os.getenv("DB_PORT", "3306"))
DB_NAME = os.getenv("DB_NAME", "grupo06_db")
DB_USER = os.getenv("DB_USER", "root")
DB_PASSWORD = os.getenv("DB_PASSWORD", "12345678")

# Caminho do script de seed (TC-01). Ajuste se o seu arquivo estiver em outro lugar.
# Pode ser um caminho absoluto ou relativo à raiz do projeto Java.
SQL_SEED_PATH = os.getenv("SQL_SEED_PATH", "../db/grupo06_db.sql")

# Caminho do executável "mysql" (cliente de linha de comando), usado só no TC-01.
MYSQL_CLI_PATH = os.getenv("MYSQL_CLI_PATH", "mysql")

# ----------------------------------------------------------------------
# Selenium
# ----------------------------------------------------------------------
# "edge" (padrão, combina com o navegador que você usa) ou "chrome"/"firefox".
BROWSER = os.getenv("BROWSER", "edge")
HEADLESS = os.getenv("HEADLESS", "false").lower() == "true"
SELENIUM_TIMEOUT = int(os.getenv("SELENIUM_TIMEOUT", "10"))

# ----------------------------------------------------------------------
# Flags de segurança
# ----------------------------------------------------------------------
# TC-17 exige testar a tela SEM nenhum cliente/produto cadastrado. Isso é
# destrutivo num banco com dados reais, então fica desligado por padrão.
# Rode com RUN_DESTRUCTIVE_TESTS=true só num banco de teste/descartável.
RUN_DESTRUCTIVE_TESTS = os.getenv("RUN_DESTRUCTIVE_TESTS", "false").lower() == "true"
