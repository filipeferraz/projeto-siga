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
package br.gov.jfrj.siga.ex.util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.jfree.util.Log;

import br.gov.jfrj.siga.base.AplicacaoException;
import br.gov.jfrj.siga.base.Correio;
import br.gov.jfrj.siga.base.SigaBaseProperties;
import br.gov.jfrj.siga.cp.CpTipoConfiguracao;
import br.gov.jfrj.siga.dp.DpPessoa;
import br.gov.jfrj.siga.ex.ExMovimentacao;
import br.gov.jfrj.siga.ex.ExTipoMovimentacao;
import br.gov.jfrj.siga.ex.bl.Ex;
import br.gov.jfrj.siga.hibernate.ExDao;

public class Notificador {

	public static int TIPO_NOTIFICACAO_GRAVACAO = 1;
	public static int TIPO_NOTIFICACAO_CANCELAMENTO = 2;
	public static int TIPO_NOTIFICACAO_EXCLUSAO = 3;

	// private static String servidor = "localhost:8080"; // teste

	// private static String servidor = "siga"; // produ��o

	// private static String servidor = "http://"
	// + SigaBaseProperties.getString(SigaBaseProperties
	// .getString("ambiente") + ".servidor.principal");

	private static String servidor = SigaBaseProperties.getString("siga.ex."
					+ SigaBaseProperties.getString("ambiente") + ".url");

	/**
	 * M�todo que notifica as pessoas com perfis vinculados ao documento.
	 * 
	 * @param mov
	 * @param tipoNotificacao
	 *            - Se � uma grava��o, cancelamento ou exclus�o de movimenta��o.
	 * @throws AplicacaoException
	 */
	public static void notificarPerfisVinculados(ExMovimentacao mov,
			int tipoNotificacao) throws AplicacaoException {
		
		if(mov.getExMobil().isApensadoAVolumeDoMesmoProcesso())
			return;

		StringBuilder conteudo = new StringBuilder(); // armazena o corpo do
		// e-mail
		StringBuilder conteudoHTML = new StringBuilder(); // armazena o corpo
		// do e-mail no formato HTML

		// lista de destinat�rios com informa��es adicionais de perfil e c�digo
		// da movimenta��o que adicionou o perfil para permitir o cancelamento.
		HashSet<Destinatario> destinatariosEmail = new HashSet<Destinatario>();

		List<Notificacao> notificacoes = new ArrayList<Notificacao>();

		/*
		 * Para cada movimenta��o do mobil geral (onde fica as movimenta��es
		 * vincula��es de perfis) verifica se: 1) A movimenta��o n�o est�
		 * cancelada; 2) Se a movimenta��o � uma vincula��o de perfil; 3) Se h�
		 * uma configura��o permitindo a notifica��o por e-mail.
		 * 
		 * Caso TODAS as condi��es acima sejam verdadeiras, adiciona o e-mail �
		 * lista de destinat�rios.
		 */
		for (ExMovimentacao m : mov.getExDocumento().getMobilGeral()
				.getExMovimentacaoSet()) {
			if (!m.isCancelada()
					&& m.getExTipoMovimentacao()
							.getIdTpMov()
							.equals(ExTipoMovimentacao.TIPO_MOVIMENTACAO_VINCULACAO_PAPEL)) {

				incluirDestinatarioPerfil(mov, destinatariosEmail, m);

			}
		}

		for (Destinatario dest : destinatariosEmail) {
			prepararTextoPapeisVinculados(dest, conteudo, conteudoHTML, mov,
					tipoNotificacao);

			String html = conteudoHTML.toString();
			String txt = conteudo.toString();

			conteudoHTML.delete(0, conteudoHTML.length());
			conteudo.delete(0, conteudo.length());

			Notificacao not = new Notificacao(dest, html, txt);
			notificacoes.add(not);
		}

		notificarPorEmail(notificacoes);

	}

	/**
	 * Inclui destinat�rios na lista baseando-se nas configura��es existentes na
	 * tabela EX_CONFIGURACAO
	 * 
	 * @param mov
	 * @param destinatariosEmail
	 * @param m
	 * @throws AplicacaoException
	 */
	private static ExDao dao() {
		return ExDao.getInstance();
	}

	private static void incluirDestinatarioPerfil(ExMovimentacao mov,
			HashSet<Destinatario> destinatariosEmail, ExMovimentacao m)
			throws AplicacaoException {

		try {

			if (m.getSubscritor() != null) {
				if (Ex.getInstance()
						.getConf()
						.podePorConfiguracao(
								mov.getExDocumento().getExFormaDocumento()
										.getExTipoFormaDoc(),
								m.getExPapel(),
								m.getSubscritor().getPessoaAtual(),
								mov.getExTipoMovimentacao(),
								CpTipoConfiguracao.TIPO_CONFIG_NOTIFICAR_POR_EMAIL)

						/*
						 * Se a movimenta��o � um cancelamento de uma
						 * movimenta��o que pode ser notificada, adiciona o
						 * e-mail.
						 */
						|| (mov.getExMovimentacaoRef() != null && Ex
								.getInstance()
								.getConf()
								.podePorConfiguracao(
										mov.getExDocumento()
												.getExFormaDocumento()
												.getExTipoFormaDoc(),
										m.getExPapel(),
										m.getSubscritor().getPessoaAtual(),
										mov.getExMovimentacaoRef()
												.getExTipoMovimentacao(),
										CpTipoConfiguracao.TIPO_CONFIG_NOTIFICAR_POR_EMAIL))) {
					destinatariosEmail.add(new Destinatario(m.getSubscritor()
							.getEmailPessoaAtual(), m
							.getSubscritor().getPessoaAtual().getSigla(), m
							.getExPapel().getDescPapel(), m.getIdMov()));
				}
			} else {
				if (m.getLotaSubscritor() != null) {
					if (Ex.getInstance()
							.getConf()
							.podePorConfiguracao(
									mov.getExDocumento().getExFormaDocumento()
											.getExTipoFormaDoc(),
									m.getExPapel(),
									m.getLotaSubscritor().getLotacaoAtual(),
									mov.getExTipoMovimentacao(),
									CpTipoConfiguracao.TIPO_CONFIG_NOTIFICAR_POR_EMAIL)

							/*
							 * Se a movimenta��o � um cancelamento de uma
							 * movimenta��o que pode ser notificada, adiciona o
							 * e-mail.
							 */
							|| (mov.getExMovimentacaoRef() != null && Ex
									.getInstance()
									.getConf()
									.podePorConfiguracao(
											mov.getExDocumento()
													.getExFormaDocumento()
													.getExTipoFormaDoc(),
											m.getExPapel(),
											m.getLotaSubscritor()
													.getLotacaoAtual(),
											mov.getExMovimentacaoRef()
													.getExTipoMovimentacao(),
											CpTipoConfiguracao.TIPO_CONFIG_NOTIFICAR_POR_EMAIL))) {
						adicionarDestinatariosEmail(mov, destinatariosEmail, m);
					}

				}

			}

		} catch (Exception e) {
			throw new AplicacaoException(
					"Erro ao enviar email de notifica��o de movimenta��o.", 0,
					e);
		}
	}

	private static void adicionarDestinatariosEmail(ExMovimentacao mov,
			HashSet<Destinatario> destinatariosEmail, ExMovimentacao m)
			throws AplicacaoException, Exception {
		List<String> listaDeEmails = dao().consultarEmailNotificacao(
				m.getLotaSubscritor().getLotacaoAtual());

		String sigla = m.getLotaSubscritor().getLotacaoAtual().getSigla();

		if (listaDeEmails.size() > 0) {

			for (String email : listaDeEmails) {

				// Caso exista alguma configura��o com email
				// nulo, significa que deve ser enviado para
				// todos da lota��o

				if (email == null) {
					for (DpPessoa pes : dao().pessoasPorLotacao(
							m.getLotaSubscritor().getLotacaoAtual()
									.getIdLotacao(), false, true)) {

						if (Ex.getInstance()
								.getConf()
								.podePorConfiguracao(
										mov.getExDocumento()
												.getExFormaDocumento()
												.getExTipoFormaDoc(),
										m.getExPapel(),
										pes,
										mov.getExTipoMovimentacao(),
										CpTipoConfiguracao.TIPO_CONFIG_NOTIFICAR_POR_EMAIL))
							destinatariosEmail
									.add(new Destinatario(pes
											.getEmailPessoaAtual(), sigla, m
											.getExPapel().getDescPapel(), m
											.getIdMov()));
					}
				} else {
					destinatariosEmail.add(new Destinatario(email, sigla, m
							.getExPapel().getDescPapel(), m.getIdMov()));
				}
			}
		} else {
			for (DpPessoa pes : dao().pessoasPorLotacao(
					m.getLotaSubscritor().getLotacaoAtual().getIdLotacao(),
					false, true)) {
				if (Ex.getInstance()
						.getConf()
						.podePorConfiguracao(
								mov.getExDocumento().getExFormaDocumento()
										.getExTipoFormaDoc(),
								m.getExPapel(),
								pes,
								mov.getExTipoMovimentacao(),
								CpTipoConfiguracao.TIPO_CONFIG_NOTIFICAR_POR_EMAIL))
					destinatariosEmail.add(new Destinatario(pes
							.getEmailPessoaAtual(), sigla, m
							.getExPapel().getDescPapel(), m.getIdMov()));
			}

		}
	}

	/**
	 * Notifica os destinat�rios assincronamente.
	 * 
	 * @param conteudo
	 *            - texto do e-mail
	 * @param conteudoHTML
	 *            - texto do e-mail no formato HTML
	 * @param destinatariosEmail
	 *            - Conjunto de destinat�rios.
	 */
	private static void notificarPorEmail(List<Notificacao> notificacoes) {
		// Se existirem destinat�rios, envia o e-mail. O e-mail � enviado
		// assincronamente.
		if (notificacoes.size() > 0) {
			CorreioThread t = new CorreioThread(notificacoes);
			t.start();
		}
	}

	/**
	 * Monta o corpo do e-mail que ser� recebido pelas pessoas com perfis
	 * vinculados.
	 * 
	 * @param dest
	 * 
	 * @param conteudo
	 * @param conteudoHTML
	 * @param mov
	 * @param tipoNotificacao
	 *            - Se � uma grava��o, cancelamento ou exclus�o de movimenta��o.
	 */
	private static void prepararTextoPapeisVinculados(Destinatario dest,
			StringBuilder conteudo, StringBuilder conteudoHTML,
			ExMovimentacao mov, int tipoNotificacao) {

		// conte�do texto
		conteudo.append("O documento ");
		conteudo.append(mov.getExMobil().getSigla());
		conteudo.append(", com descri��o '");
		conteudo.append(mov.getExDocumento().getDescrDocumento());

		if (tipoNotificacao == TIPO_NOTIFICACAO_GRAVACAO) {

			conteudo.append("', recebeu a movimenta��o '"
					+ mov.getExTipoMovimentacao().getDescricao() + "'. ");
		}
		if (tipoNotificacao == TIPO_NOTIFICACAO_CANCELAMENTO) {

			conteudo.append("', recebeu a movimenta��o '"
					+ mov.getExMovimentacaoRef().getDescrTipoMovimentacao()
					+ "'. ");
		}
		if (tipoNotificacao == TIPO_NOTIFICACAO_EXCLUSAO) {

			conteudo.append("',  recebeu movimenta��o de exclus�o de '"
					+ mov.getExTipoMovimentacao().getDescricao() + "'. ");
		}

		if (mov.getCadastrante() != null) {
			conteudo.append("A movimenta��o foi realizada por '"
					+ mov.getCadastrante().getNomePessoa() + " (Matr�cula: "
					+ mov.getCadastrante().getMatricula() + ")'.\n\n");

		}
		conteudo.append("Para visualizar o documento, ");
		conteudo.append("clique no link abaixo:\n\n");
		conteudo.append(servidor
				+ "/expediente/doc/exibir.action?sigla=");
		conteudo.append(mov.getExDocumento().getSigla());

		conteudo.append("\n\nEste email foi enviado porque ");
		conteudo.append(dest.sigla);
		conteudo.append(" tem o perfil de '");
		conteudo.append(dest.papel);
		conteudo.append("' no documento ");
		conteudo.append(mov.getExDocumento().getSigla());
		conteudo.append(". Caso n�o deseje mais receber notifica��es desse documento, clique no link abaixo para se descadastrar:\n\n");
		conteudo.append(servidor + "/expediente/mov/cancelar.action?id=");
		conteudo.append(dest.idMovPapel);

		// conte�do html
		conteudoHTML.append("<html><body>");

		conteudoHTML.append("<p>O documento <b>");
		conteudoHTML.append(mov.getExMobil().getSigla());
		conteudoHTML.append("</b>, com descri��o '<b>");
		conteudoHTML.append(mov.getExDocumento().getDescrDocumento());

		if (tipoNotificacao == TIPO_NOTIFICACAO_GRAVACAO) {
			conteudoHTML.append("</b>', recebeu a movimenta��o <b>"
					+ mov.getExTipoMovimentacao().getDescricao() + "</b>.");
		}
		if (tipoNotificacao == TIPO_NOTIFICACAO_CANCELAMENTO) {
			conteudoHTML.append("</b>', recebeu a movimenta��o <b>"
					+ mov.getExMovimentacaoRef().getDescrTipoMovimentacao()
					+ "</b>.");
		}
		if (tipoNotificacao == TIPO_NOTIFICACAO_EXCLUSAO) {
			conteudoHTML
					.append("</b>', recebeu movimenta��o de exclus�o de <b>"
							+ mov.getExTipoMovimentacao().getDescricao()
							+ "</b>.");
		}

		if (mov.getCadastrante() != null) {
			conteudoHTML.append("<br/>A movimenta��o foi realizada por <b>"
					+ mov.getCadastrante().getNomePessoa() + " (Matr�cula: "
					+ mov.getCadastrante().getMatricula() + ")</b></p>");
		}

		if (mov.getExTipoMovimentacao().getIdTpMov() == ExTipoMovimentacao.TIPO_MOVIMENTACAO_TRANSFERENCIA
				|| mov.getExTipoMovimentacao().getIdTpMov() == ExTipoMovimentacao.TIPO_MOVIMENTACAO_DESPACHO_TRANSFERENCIA) {
			if (mov.getResp() != null) {
				conteudoHTML
						.append("<br/>O destinat�rio da transfer�ncia � <b>"
								+ mov.getResp().getNomePessoa()
								+ " (Matr�cula: "
								+ mov.getResp().getMatricula() + ") do (a) "
								+ mov.getLotaResp().getNomeLotacao()
								+ " (sigla: " + mov.getLotaResp().getSigla()
								+ ")" + "</b></p>");
			} else {
				conteudoHTML
						.append("<br/>A lota��o de destino da transfer�ncia � <b>"
								+ mov.getLotaResp().getNomeLotacao()
								+ " (sigla: "
								+ mov.getLotaResp().getSigla()
								+ ")" + "</b></p>");

			}
		}

		conteudoHTML.append("<p>Para visualizar o documento, ");
		conteudoHTML.append("clique <a href=\"");
		conteudoHTML.append(servidor
				+ "/expediente/doc/exibir.action?sigla=");
		conteudoHTML.append(mov.getExDocumento().getSigla());
		conteudoHTML.append("\">aqui</a>.</p>");

		conteudoHTML.append("<p>Este email foi enviado porque ");
		conteudoHTML.append(dest.sigla);
		conteudoHTML.append(" tem o perfil de '");
		conteudoHTML.append(dest.papel);
		conteudoHTML.append("' no documento ");
		conteudoHTML.append(mov.getExDocumento().getSigla());
		conteudoHTML
				.append(". Caso n�o deseje mais receber notifica��es desse documento, clique <a href=\"");
		conteudoHTML.append(servidor
				+ "/expediente/mov/cancelar.action?id=");
		conteudoHTML.append(dest.idMovPapel);
		conteudoHTML.append("\">aqui</a> para descadastrar.</p>");

		conteudoHTML.append("</body></html>");

	}

	/**
	 * Classe que representa um thread de envio de e-mail. H� a necessidade do
	 * envio de e-mail ser ass�ncrono, caso contr�rio, o usu�rio sentir� uma
	 * degrada��o de performance.
	 * 
	 * @author kpf
	 * 
	 */
	static class CorreioThread extends Thread {

		private List<Notificacao> notificacoes;

		public CorreioThread(List<Notificacao> notificacoes) {
			super();
			this.notificacoes = notificacoes;
		}

		@Override
		public void run() {
			try {
				for (Notificacao not : notificacoes)
					Correio.enviar(
							SigaBaseProperties
									.getString("servidor.smtp.usuario.remetente"),
							new String[] { not.dest.email },
							"Notifica��o Autom�tica - Movimenta��o de Documento",
							not.txt, not.html);
			} catch (Exception e) {
				Log.error(e);
			}
		}

	}

	static class Notificacao {
		Destinatario dest;
		String html;
		String txt;

		public Notificacao(Destinatario dest, String html, String txt) {
			super();
			this.dest = dest;
			this.html = html;
			this.txt = txt;
		}
	}

	static class Destinatario implements Comparable<Destinatario> {

		public String email;
		public String sigla;
		public String papel;
		public long idMovPapel;

		public Destinatario(String email, String sigla, String papel,
				long idMovPapel) {
			super();
			this.email = email;
			this.sigla = sigla;
			this.papel = papel;
			this.idMovPapel = idMovPapel;
		}

		public int compareTo(Destinatario o) {
			// TODO Auto-generated method stub
			return email.compareTo(o.email);
		}

	}

}
