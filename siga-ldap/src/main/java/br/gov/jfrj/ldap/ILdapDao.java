/*******************************************************************************
 * Copyright (c) 2006 - 2011 SJRJ.
 * 
 *     This file is part of SIGA.
 * 
 *     SIGA is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 * 
 *     SIGA is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 * 
 *     You should have received a copy of the GNU General Public License
 *     along with SIGA.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package br.gov.jfrj.ldap;

import java.io.UnsupportedEncodingException;
import java.util.List;

import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.ldap.LdapContext;

import br.gov.jfrj.siga.base.AplicacaoException;

public interface ILdapDao {

	public static final int PORTA_SSL = 636;
	public static final int PORTA_COMUM = 389;
	public final int UF_ACCOUNTDISABLE = 0x0002;
	public final int UF_PASSWD_NOTREQD = 0x0020;
	public final int UF_PASSWD_CANT_CHANGE = 0x0040;
	public final int ADS_GROUP_TYPE_SYSTEM = 0x0001;
	public final int ADS_GROUP_TYPE_GLOBAL_GROUP = 0x0002;
	public final int ADS_GROUP_TYPE_DOMAIN_LOCAL_GROUP = 0x0004;
	public final int ADS_GROUP_TYPE_LOCAL_GROUP = 0x0004;
	public final int ADS_GROUP_TYPE_UNIVERSAL_GROUP = 0x0008;
	public final int ADS_GROUP_TYPE_SECURITY_ENABLED = 0x80000000;
	public final int ADS_INSTANCE_TYPE_HEAD = 1;
	public final int ADS_INSTANCE_TYPE_REPLICA_NOT_INSTANTIATED = 2;
	public final int ADS_INSTANCE_TYPE_WRITABLE = 4;
	public final int ADS_INSTANCE_TYPE_ABOVE = 8;
	public final int ADS_INSTANCE_TYPE_FIRST_TIME = 10;
	public final int ADS_INSTANCE_TYPE_REMOVED = 20;

	public abstract boolean isSomenteLeitura();

	public abstract void conectarComSSL(String servidor, String porta,
                                        String usuario, String senha, String caminhoKeystore)
			throws AplicacaoException;

	public abstract void conectarSemSSL(String servidor, String porta,
                                        String usuario, String senha) throws AplicacaoException;

	public abstract void incluir(String dn, Attributes atributos)
			throws AplicacaoException;

	public abstract void excluir(String dn) throws AplicacaoException;

	public abstract void alterar(String dn, Attributes atributos)
			throws AplicacaoException;

	public abstract Attributes pesquisar(String dn) throws AplicacaoException;

	public abstract void definirSenha(String dnUsuario, String senhaNova)
			throws AplicacaoException;

	/**
	 * Define se uma conta est� ativa
	 * 
	 * @param dnUsuario
	 * @throws AplicacaoException
	 */
	public abstract void ativarUsuario(String dnUsuario)
			throws AplicacaoException;

	/**
	 * Define se uma conta est� n�o est� ativa
	 * 
	 * @param dnUsuario
	 * @throws AplicacaoException
	 */
	public abstract void desativarUsuario(String dnUsuario)
			throws AplicacaoException;

	/**
	 * Verifica se um objeto existe em algum objeto da �rvore LDAP
	 * 
	 * @param cn
	 *            - Common Name do objeto a ser encontrado
	 * @return - true se o objeto existe.
	 */
	public abstract boolean existe(String cn);

	/**
	 * Muda a localiza��o de um objeto na �rvore LDAP
	 * 
	 * @param dn
	 * @param novoDN
	 */
	public abstract void mover(String dn, String novoDN) throws AplicacaoException;

	/**
	 * Verifica se � um grupo de seguran�a.
	 * Ref:http://msdn.microsoft.com/en-us/library/ms675935(VS.85).aspx
	 * 
	 * @param dn
	 * @return
	 * @throws javax.naming.NamingException
	 */
	public abstract boolean isGrupoSeguranca(String dn);

	/**
	 * Verifica se � um grupo de distribui��o.
	 * Ref:http://msdn.microsoft.com/en-us/library/ms675935(VS.85).aspx
	 * 
	 * @param dn
	 * @return
	 * @throws AplicacaoException
	 * @throws javax.naming.NamingException
	 * @throws javax.naming.NamingException
	 */
	public abstract boolean isGrupoDistribuicao(String dn);

	public abstract boolean isGrupoSistema(String dn) throws AplicacaoException;

	public abstract boolean isGrupoGlobal(String dn);

	public abstract boolean isGrupo(String dn);

	public abstract Attributes getAttributes(String dn);

	public abstract boolean isGrupoDomainLocal(String dn);

	public abstract boolean isGrupoUniversal(String dn);

	public abstract boolean isUsuario(String dn) throws AplicacaoException;

	public abstract void inserirValorAtributoMultivalorado(String dn,
                                                           String nomeAtributo, Object valorAtributo)
			throws AplicacaoException;

	public abstract void removerValorAtributoMultivalorado(String dn,
                                                           String nomeAtributo, Object valorAtributo)
			throws AplicacaoException;

	public abstract void alterarAtributo(String dn, String nomeAtributo,
                                         Object valorAtributo) throws AplicacaoException;
	
	public abstract void excluirAtributo(String dn, String nomeAtributo) throws AplicacaoException;

	public abstract LdapContext getContexto();

	/**
	 * Verifica se um usuario pode se autenticar na �rvore LDAP. Esse m�todo �
	 * mais simples que o conectarComSSL() e conectarSemSSL() e tem o prop�sito
	 * apenas de confirmar se o usu�rio/senha est�o corretos. Este m�todo n�o
	 * possibilita o uso de ssl, n�o retorna o contexto LDAP para o usu�rio
	 * manipular e n�o permite informar o DN para autentica��o. Se for essa a
	 * sua necessidade, use o m�todo conectarComSSL() ou conectarSemSSL().
	 * 
	 * @param usuario
	 *            - nome do usu�rio a se logar, na JFRJ � a sigla da pessoa (ex:
	 *            kpf)
	 * @param dominio
	 *            - dom�nio do AD
	 * @param senha
	 *            - senha do usu�rio
	 * @param servidor
	 *            - servidor de autentica��o
	 * @param porta
	 *            - porta do servidor de autentica��o (padr�o: 389)
	 * @return true - se o login foi bem sucedido. <br/>
	 *         false - em caso de falha.
	 */
	public abstract boolean verificarConexao(String usuario, String dominio,
                                             String senha, String servidor, String porta);

	/**
	 * Altera a senha de um usu�rio. � necess�rio informar a senha antiga.
	 * 
	 * Alterar a senha � uma opera��o de mudan�a do ldap que deleta a senha
	 * antiga e adiciona a nova senha. http://support.microsoft.com/kb/269190
	 * 
	 * 
	 * @param dnUsuario
	 * @param senhaAntiga
	 * @param senhaNova
	 * @throws AplicacaoException
	 * @throws java.io.UnsupportedEncodingException
	 * @throws javax.naming.NamingException
	 * @throws AplicacaoException
	 */
	public abstract void alterarSenha(String dnUsuario, String senhaAntiga,
                                      String senhaNova) throws AplicacaoException;

	public void criarUsuario(String login, String nomeUsuario,
                             String dnPontoCriacao) throws AplicacaoException;

	public void criarContato(String nome, String dnPontoCriacao)
			throws AplicacaoException;

	public void criarUnidadeOrganizacional(String nome, String dnPontoCriacao)
			throws AplicacaoException;

	public void criarGrupoSeguranca(String nome, String dnPontoCriacao)
			throws AplicacaoException;

	public void criarGrupoDistribuicao(String nome, String dnPontoCriacao)
			throws AplicacaoException;

}
