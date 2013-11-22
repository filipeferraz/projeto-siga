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
package br.gov.jfrj.siga.ex.bl;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.beanutils.PropertyUtils;
import org.apache.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.ObjectNotFoundException;
import org.hibernate.cfg.AnnotationConfiguration;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.jdom.output.XMLOutputter;

import br.gov.jfrj.itextpdf.ConversorHtml;
import br.gov.jfrj.itextpdf.Documento;
import br.gov.jfrj.siga.Service;
import br.gov.jfrj.siga.base.AplicacaoException;
import br.gov.jfrj.siga.base.Contexto;
import br.gov.jfrj.siga.base.Correio;
import br.gov.jfrj.siga.base.SigaBaseProperties;
import br.gov.jfrj.siga.cd.service.CdService;
import br.gov.jfrj.siga.cp.CpConfiguracao;
import br.gov.jfrj.siga.cp.CpIdentidade;
import br.gov.jfrj.siga.cp.CpTipoConfiguracao;
import br.gov.jfrj.siga.cp.bl.Cp;
import br.gov.jfrj.siga.cp.bl.CpAmbienteEnumBL;
import br.gov.jfrj.siga.cp.bl.CpBL;
import br.gov.jfrj.siga.dp.CpMarca;
import br.gov.jfrj.siga.dp.CpMarcador;
import br.gov.jfrj.siga.dp.CpOrgao;
import br.gov.jfrj.siga.dp.CpOrgaoUsuario;
import br.gov.jfrj.siga.dp.DpLotacao;
import br.gov.jfrj.siga.dp.DpPessoa;
import br.gov.jfrj.siga.dp.DpResponsavel;
import br.gov.jfrj.siga.dp.dao.CpDao;
import br.gov.jfrj.siga.ex.ExArquivo;
import br.gov.jfrj.siga.ex.ExArquivoNumerado;
import br.gov.jfrj.siga.ex.ExBoletimDoc;
import br.gov.jfrj.siga.ex.ExClassificacao;
import br.gov.jfrj.siga.ex.ExConfiguracao;
import br.gov.jfrj.siga.ex.ExDocumento;
import br.gov.jfrj.siga.ex.ExFormaDocumento;
import br.gov.jfrj.siga.ex.ExMarca;
import br.gov.jfrj.siga.ex.ExMobil;
import br.gov.jfrj.siga.ex.ExModelo;
import br.gov.jfrj.siga.ex.ExMovimentacao;
import br.gov.jfrj.siga.ex.ExNivelAcesso;
import br.gov.jfrj.siga.ex.ExPapel;
import br.gov.jfrj.siga.ex.ExSituacaoConfiguracao;
import br.gov.jfrj.siga.ex.ExTemporalidade;
import br.gov.jfrj.siga.ex.ExTipoDespacho;
import br.gov.jfrj.siga.ex.ExTipoDocumento;
import br.gov.jfrj.siga.ex.ExTipoFormaDoc;
import br.gov.jfrj.siga.ex.ExTipoMobil;
import br.gov.jfrj.siga.ex.ExTipoMovimentacao;
import br.gov.jfrj.siga.ex.ExVia;
import br.gov.jfrj.siga.ex.SigaExProperties;
import br.gov.jfrj.siga.ex.ext.AbstractConversorHTMLFactory;
import br.gov.jfrj.siga.ex.util.DatasPublicacaoDJE;
import br.gov.jfrj.siga.ex.util.GeradorRTF;
import br.gov.jfrj.siga.ex.util.MascaraUtil;
import br.gov.jfrj.siga.ex.util.Notificador;
import br.gov.jfrj.siga.ex.util.ProcessadorHtml;
import br.gov.jfrj.siga.ex.util.ProcessadorModelo;
import br.gov.jfrj.siga.ex.util.ProcessadorModeloFreemarker;
import br.gov.jfrj.siga.ex.util.PublicacaoDJEBL;
import br.gov.jfrj.siga.hibernate.ExDao;
import br.gov.jfrj.siga.model.Selecionavel;
import br.gov.jfrj.siga.model.dao.HibernateUtil;
import br.gov.jfrj.siga.parser.SiglaParser;
import br.gov.jfrj.siga.wf.service.WfService;

import com.sun.org.apache.xerces.internal.impl.dv.util.Base64;

public class ExBL extends CpBL {

	private final String SHA1 = "1.3.14.3.2.26";

	private final boolean BUSCAR_CARIMBO_DE_TEMPO = false;
	private final boolean VALIDAR_LCR = false;

	private final ThreadLocal<SortedSet<ExMobil>> threadAlteracaoParcial = new ThreadLocal<SortedSet<ExMobil>>();

	private ProcessadorModelo processadorModeloJsp;
	private ProcessadorModelo processadorModeloFreemarker = new ProcessadorModeloFreemarker();

	private final static Logger log = Logger.getLogger(ExBL.class);

	public ThreadLocal<SortedSet<ExMobil>> getThreadAlteracaoParcial() {
		return threadAlteracaoParcial;
	}

	public ExCompetenciaBL getComp() {
		return (ExCompetenciaBL) super.getComp();
	}

	public ExConfiguracaoBL getConf() {
		return (ExConfiguracaoBL) super.getComp().getConfiguracaoBL();
	}

	public ProcessadorModelo getProcessadorModeloJsp() {
		return processadorModeloJsp;
	}

	public void setProcessadorModeloJsp(ProcessadorModelo processadorModelo) {
		this.processadorModeloJsp = processadorModelo;
	}

	public String fixTableCols(String html) throws Exception {
		StringReader st = new StringReader(html);
		SAXBuilder sb = new SAXBuilder();
		sb.setIgnoringElementContentWhitespace(true);
		Document doc = sb.build(st);
		Element raiz = doc.getRootElement();
		fixTableCols(raiz);
		return (new XMLOutputter()).outputString(doc);
	}

	private void fixTableCols(Element raiz) {
		List<Element> filhos = raiz.getChildren("table");
		for (Element table : filhos) {

			if (table.getAttribute("width") == null)
				table.setAttribute("width", "50%");

			if (table.getAttribute("border") == null
					&& !(table.getAttribute("style") != null && table
							.getAttribute("style").getValue()
							.contains("border")))
				table.setAttribute("border", "1");

			Element tr = null;
			Element tableBody = table.getChild("tbody");
			if (tableBody == null)
				tableBody = table.getChild("thead");
			if (tableBody == null)
				tr = (Element) table.getChildren("tr").get(0);
			else
				tr = (Element) tableBody.getChildren("tr").get(0);

			List<Element> tds = tr.getChildren("td");
			int cols = 0;
			for (Element td : tds) {
				Attribute colspan = td.getAttribute("colspan");
				if (colspan != null)
					cols += (new Integer(colspan.getValue()));
				else
					cols++;
				fixTableCols(td);
			}

			Element colgroup = new Element("colgroup");
			for (int i = 0; i < cols; i++) {
				Element col = new Element("col");
				col.setAttribute("width", 100 / cols + "%");
				colgroup.addContent(col);
			}
			table.addContent(0, colgroup);
		}
	}

	// Executa algoritmo de compara��o entre dois sets e
	// preenche as listas: inserir, excluir e atualizar.
	// \
	private void encaixar(SortedSet<ExMarca> setA, SortedSet<ExMarca> setB,
			Set<ExMarca> incluir, Set<ExMarca> excluir) {
		Iterator<ExMarca> ia = setA.iterator();
		Iterator<ExMarca> ib = setB.iterator();

		ExMarca a = null;
		ExMarca b = null;

		if (ia.hasNext())
			a = ia.next();
		if (ib.hasNext())
			b = ib.next();
		while (a != null || b != null) {
			if ((a == null) || (b != null && a.compareTo(b) > 0)) {
				// Existe em setB, mas nao existe em setA
				incluir.add(b);
				if (ib.hasNext())
					b = ib.next();
				else
					b = null;
			} else if (b == null || (a != null && b.compareTo(a) > 0)) {
				// Existe em setA, mas nao existe em setB
				excluir.add(a);
				if (ia.hasNext())
					a = ia.next();
				else
					a = null;
			} else {

				if (a == null) {
					int i = 0;
				}
				// O registro existe nos dois
				// atualizar.add(new Par(b, a));
				if (ib.hasNext())
					b = ib.next();
				else
					b = null;
				if (ia.hasNext())
					a = ia.next();
				else
					a = null;
			}
		}
		ib = null;
		ia = null;
	}

	public void atualizarMarcas(ExDocumento doc) {
		for (ExMobil mob : doc.getExMobilSet()) {
			SortedSet<ExMarca> setA = mob.getExMarcaSet();
			if (setA == null)
				setA = new TreeSet<ExMarca>();
			SortedSet<ExMarca> setB = calcularMarcadores(mob);
			Set<ExMarca> incluir = new TreeSet<ExMarca>();
			Set<ExMarca> excluir = new TreeSet<ExMarca>();
			encaixar(setA, setB, incluir, excluir);
			for (ExMarca i : incluir) {
				if (i.getExMobil().getExMarcaSet() == null) {
					i.getExMobil().setExMarcaSet(new TreeSet<ExMarca>());
				}
				i.getExMobil().getExMarcaSet().add(i);
				dao().gravar(i);
			}
			for (ExMarca e : excluir) {
				if (e.getExMobil().getExMarcaSet() == null) {
					e.getExMobil().setExMarcaSet(new TreeSet<ExMarca>());
				}
				e.getExMobil().getExMarcaSet().remove(e);
				dao().excluir(e);
			}
		}
	}

	private void acrescentarMarca(SortedSet<ExMarca> set, ExMobil mob,
			Long idMarcador, Date dt, DpPessoa pess, DpLotacao lota) {
		ExMarca mar = new ExMarca();
		mar.setExMobil(mob);
		mar.setCpMarcador(dao().consultar(idMarcador, CpMarcador.class, false));
		if (pess != null)
			mar.setDpPessoaIni(pess.getPessoaInicial());
		if (lota != null)
			mar.setDpLotacaoIni(lota.getLotacaoInicial());
		mar.setDtIniMarca(dt);
		set.add(mar);
	}

	public void main(String args[]) throws Exception {

		String classificacao = "00.01.01.01";

		final Pattern p2 = Pattern
				.compile("^([0-9][0-9]).?([0-9][0-9]).?([0-9][0-9]).?([0-9][0-9])");

		Matcher m = p2.matcher(classificacao);
		boolean a = m.find();

		CpAmbienteEnumBL ambiente = CpAmbienteEnumBL.DESENVOLVIMENTO;
		Cp.getInstance().getProp().setPrefixo(ambiente.getSigla());

		AnnotationConfiguration cfg = ExDao.criarHibernateCfg(ambiente);
		HibernateUtil.configurarHibernate(cfg, "");
		marcarTudo(90000);
	}

	public void marcar(ExDocumento doc) throws Exception {
		ExDao.iniciarTransacao();
		atualizarMarcas(doc);
		for (ExMovimentacao m : doc.getExMovimentacaoSet()) {
			m.setNumPaginas(m.getContarNumeroDePaginas());
			dao().gravar(m);
		}
		ExDao.commitTransacao();
	}

	/**
	 * M�todo criado para contar o n�mero de p�ginas de um documento que foi
	 * criado antes da fun��o que grava um documento com o total de p�ginas.
	 * 
	 * @param doc
	 * @return
	 * @throws Exception
	 */
	public Integer ContarNumeroDePaginas(ExDocumento doc) {
		ExDao.iniciarTransacao();
		Integer numeroDePaginas = doc.getContarNumeroDePaginas();
		doc.setNumPaginas(numeroDePaginas);
		dao().gravar(doc);
		try {
			ExDao.commitTransacao();
		} catch (Throwable e) {
			System.out
					.println("Erro ao contar o n�mero de p�ginas do documento."
							+ doc);
			e.printStackTrace();
		}

		return numeroDePaginas;
	}

	/**
	 * M�todo criado para contar o n�mero de p�ginas de uma movimentacao que foi
	 * criada antes da fun��o que grava uma movimentacao com o total de p�ginas.
	 * 
	 * @param doc
	 * @return
	 * @throws Exception
	 */
	public Integer ContarNumeroDePaginas(ExMovimentacao mov) {
		ExDao.iniciarTransacao();
		Integer numeroDePaginas = mov.getContarNumeroDePaginas();
		mov.setNumPaginas(numeroDePaginas);
		dao().gravar(mov);
		try {
			ExDao.commitTransacao();
		} catch (Throwable e) {
			System.out
					.println("Erro ao contar o n�mero de p�ginas da movimenta��o."
							+ mov);
			e.printStackTrace();
		}

		return numeroDePaginas;
	}

	public void numerarTudo(int aPartirDe) throws Exception {
		List<ExDocumento> list = new ArrayList<ExDocumento>();

		final Criteria countCrit = dao().getSessao()
				.createCriteria(ExDocumento.class)
				.add(Restrictions.gt("idDoc", new Long(aPartirDe)));
		countCrit.setProjection(Projections.rowCount());
		Integer totalDocs = ((Long) countCrit.uniqueResult()).intValue();

		final Criteria crit = dao().getSessao()
				.createCriteria(ExDocumento.class)
				.add(Restrictions.gt("idDoc", new Long(aPartirDe)));
		crit.setMaxResults(60);
		crit.addOrder(Order.asc("idDoc"));

		int index = 0;

		do {
			long inicio = System.currentTimeMillis();
			System.gc();
			iniciarAlteracao();
			crit.setFirstResult(index);
			list = crit.list();
			for (ExDocumento doc : list) {
				index++;
				try {
					for (ExMovimentacao m : doc.getExMovimentacaoSet()) {
						m.setNumPaginas(m.getContarNumeroDePaginas());
						dao().gravar(m);
					}
				} catch (Throwable e) {
					System.out.println("Erro ao marcar o doc " + doc);
					e.printStackTrace();
				}
				if (index % 50 == 0)
					System.gc();
				System.out.print(doc.getIdDoc() + " ok - ");
			}
			ExDao.commitTransacao();
			dao().getSessao().clear();
			long duracao = System.currentTimeMillis() - inicio;
			System.out.println();
			System.out.println(new SimpleDateFormat("HH:mm:ss")
					.format(new Date())
					+ " "
					+ String.valueOf(index)
					+ " numerados de " + totalDocs);
		} while (list.size() > 0);

		System.gc();
	}

	public void marcarTudo(int aPartirDe) throws Exception {

		List<ExDocumento> list = new ArrayList<ExDocumento>();

		final Criteria countCrit = dao().getSessao()
				.createCriteria(ExDocumento.class)
				.add(Restrictions.gt("idDoc", new Long(aPartirDe)));
		countCrit.setProjection(Projections.rowCount());
		Integer totalDocs = ((Long) countCrit.uniqueResult()).intValue();

		final Criteria crit = dao().getSessao()
				.createCriteria(ExDocumento.class)
				.add(Restrictions.gt("idDoc", new Long(aPartirDe)));
		crit.setMaxResults(60);
		crit.addOrder(Order.asc("idDoc"));

		int index = 0;

		do {
			long inicio = System.currentTimeMillis();
			System.gc();
			iniciarAlteracao();
			crit.setFirstResult(index);
			list = crit.list();
			for (ExDocumento doc : list) {
				index++;
				try {
					atualizarMarcas(doc);
					for (ExMovimentacao m : doc.getExMovimentacaoSet()) {
						m.setNumPaginas(m.getContarNumeroDePaginas());
						dao().gravar(m);
					}
				} catch (Throwable e) {
					System.out.println("Erro ao marcar o doc " + doc);
					e.printStackTrace();
				}
				if (index % 50 == 0)
					System.gc();
				System.out.print(doc.getIdDoc() + " ok - ");
			}
			ExDao.commitTransacao();
			dao().getSessao().clear();
			long duracao = System.currentTimeMillis() - inicio;
			System.out.println();
			System.out.println(new SimpleDateFormat("HH:mm:ss")
					.format(new Date())
					+ " "
					+ String.valueOf(index)
					+ " marcados de " + totalDocs);
		} while (list.size() > 0);

		System.gc();
	}

	/**
	 * Calcula quais as marcas cada mobil ter� com base nas movimenta��es que
	 * foram feitas no documento.
	 * 
	 * @param mob
	 */
	private SortedSet<ExMarca> calcularMarcadores(ExMobil mob) {
		SortedSet<ExMarca> set = new TreeSet<ExMarca>();

		ExMovimentacao ultMovNaoCanc = mob.getUltimaMovimentacaoNaoCancelada();

		Boolean isDocumentoSemEfeito = mob.doc().isSemEfeito();

		if (mob.isGeral()) {
			if (mob.doc().getDtFechamento() == null) {
				acrescentarMarca(set, mob, CpMarcador.MARCADOR_EM_ELABORACAO,
						mob.doc().getDtRegDoc(), mob.doc().getCadastrante(),
						mob.doc().getLotaCadastrante());
				if (mob.getExDocumento().getSubscritor() != null)
					acrescentarMarca(set, mob, CpMarcador.MARCADOR_REVISAR, mob
							.doc().getDtRegDoc(), mob.getExDocumento()
							.getSubscritor(), null);

			}

			if (mob.getExMovimentacaoSet() != null) {
				if (isDocumentoSemEfeito) {
					for (ExMovimentacao mov : mob.getExMovimentacaoSet()) {
						if (mov.isCancelada())
							continue;

						Long t = mov.getIdTpMov();
						if (t == ExTipoMovimentacao.TIPO_MOVIMENTACAO_TORNAR_SEM_EFEITO) {
							acrescentarMarca(set, mob,
									CpMarcador.MARCADOR_SEM_EFEITO,
									mov.getDtIniMov(), mov.getCadastrante(),
									mov.getLotaCadastrante());
						}
					}

				} else {
					// Verificar a situa��o no DJE
					Long mDje = null;
					ExMovimentacao movDje = null;
					for (ExMovimentacao mov : mob.getExMovimentacaoSet()) {
						if (mov.isCancelada())
							continue;
						Long m = null;
						Long t = mov.getIdTpMov();
						if (t == ExTipoMovimentacao.TIPO_MOVIMENTACAO_VINCULACAO_PAPEL) {
							switch ((int) (long) mov.getExPapel().getIdPapel()) {
							case (int) ExPapel.PAPEL_GESTOR:
								m = CpMarcador.MARCADOR_COMO_GESTOR;
								break;
							case (int) ExPapel.PAPEL_INTERESSADO:
								m = CpMarcador.MARCADOR_COMO_INTERESSADO;
								break;
							}
							if (m != null)
								acrescentarMarca(set, mob, m,
										mov.getDtIniMov(), mov.getSubscritor(),
										mov.getLotaSubscritor());
						} else if (t == ExTipoMovimentacao.TIPO_MOVIMENTACAO_PEDIDO_PUBLICACAO) {
							mDje = CpMarcador.MARCADOR_PUBLICACAO_SOLICITADA;
							movDje = mov;
						} else if (t == ExTipoMovimentacao.TIPO_MOVIMENTACAO_AGENDAMENTO_DE_PUBLICACAO) {
							mDje = CpMarcador.MARCADOR_REMETIDO_PARA_PUBLICACAO;
							movDje = mov;
						} else if (t == ExTipoMovimentacao.TIPO_MOVIMENTACAO_DISPONIBILIZACAO) {
							mDje = CpMarcador.MARCADOR_DISPONIBILIZADO;
							movDje = mov;
						} else if (t == ExTipoMovimentacao.TIPO_MOVIMENTACAO_ANEXACAO
								&& mob.doc().isEletronico()
								&& !mob.doc().jaTransferido()) {
							m = CpMarcador.MARCADOR_ANEXO_PENDENTE_DE_ASSINATURA;
							/*
							 * n�o � poss�vel usar ExMovimentacao.isAssinada()
							 * pois n�o h� tempo habil no BD de efetivar a
							 * inclusao de movimentacao de assinatura de
							 * moviment��o
							 */
							for (ExMovimentacao movAss : mob
									.getExMovimentacaoSet()) {
								if ((movAss.getExTipoMovimentacao()
										.getIdTpMov() == ExTipoMovimentacao.TIPO_MOVIMENTACAO_ASSINATURA_DIGITAL_MOVIMENTACAO || movAss
										.getExTipoMovimentacao().getIdTpMov() == ExTipoMovimentacao.TIPO_MOVIMENTACAO_CONFERENCIA_COPIA_DOCUMENTO)
										&& movAss.getExMovimentacaoRef()
												.getIdMov() == mov.getIdMov()) {
									m = null;
									break;
								}
							}
							if (m != null)
								acrescentarMarca(set, mob, m,
										mov.getDtIniMov(),
										mov.getCadastrante(),
										mov.getLotaCadastrante());
						} else if (t == ExTipoMovimentacao.TIPO_MOVIMENTACAO_INCLUSAO_DE_COSIGNATARIO) {
							if (mob.getDoc().isEletronico()) {
								if (!mob.getDoc().isAssinado())
									m = CpMarcador.MARCADOR_REVISAR;
								else {
									if (mob.getDoc().isAssinadoSubscritor())
										m = CpMarcador.MARCADOR_COMO_SUBSCRITOR;
									else
										m = CpMarcador.MARCADOR_REVISAR;
									for (ExMovimentacao assinatura : mob
											.getDoc().getTodasAsAssinaturas()) {
										if (assinatura.getSubscritor()
												.equivale(mov.getSubscritor())) {
											m = null;
											break;
										}
									}
								}
								if (m != null)
									acrescentarMarca(set, mob, m,
											mov.getDtIniMov(),
											mov.getSubscritor(), null);
							}

						}
					}
					if (mDje != null) {
						acrescentarMarca(set, mob, mDje, movDje.getDtIniMov(),
								movDje.getTitular(), movDje.getLotaTitular());
					}
				}
			}
			return set;
		} else if (ultMovNaoCanc == null) {
			ExMovimentacao ultMov = mob.getUltimaMovimentacao();
			Date dt = null;
			if (ultMov != null) {
				dt = ultMov.getDtIniMov();
			}
			acrescentarMarca(set, mob, CpMarcador.MARCADOR_CANCELADO, dt, mob
					.doc().getCadastrante(), mob.doc().getLotaCadastrante());
			return set;
		}

		if (!isDocumentoSemEfeito) {
			
			boolean apensadoAVolumeDoMesmoProcesso = mob.isApensadoAVolumeDoMesmoProcesso();

			long m = CpMarcador.MARCADOR_CANCELADO;
			long mAnterior = m;
			Date dt = null;
			for (ExMovimentacao mov : mob.getExMovimentacaoSet()) {
				if (mov.isCancelada())
					continue;
				Long t = mov.getIdTpMov();
				if (t == ExTipoMovimentacao.TIPO_MOVIMENTACAO_PEDIDO_PUBLICACAO)
					m = CpMarcador.MARCADOR_PUBLICACAO_SOLICITADA;
				if (t == ExTipoMovimentacao.TIPO_MOVIMENTACAO_DISPONIBILIZACAO)
					m = CpMarcador.MARCADOR_DISPONIBILIZADO;
				if (t == ExTipoMovimentacao.TIPO_MOVIMENTACAO_AGENDAMENTO_DE_PUBLICACAO)
					m = CpMarcador.MARCADOR_REMETIDO_PARA_PUBLICACAO;
				if (t == ExTipoMovimentacao.TIPO_MOVIMENTACAO_ARQUIVAMENTO_CORRENTE)
					m = CpMarcador.MARCADOR_ARQUIVADO_CORRENTE;
				if (t == ExTipoMovimentacao.TIPO_MOVIMENTACAO_ARQUIVAMENTO_PERMANENTE)
					m = CpMarcador.MARCADOR_ARQUIVADO_PERMANENTE;
				if (t == ExTipoMovimentacao.TIPO_MOVIMENTACAO_SOBRESTAR)
					m = CpMarcador.MARCADOR_SOBRESTADO;
				if (t == ExTipoMovimentacao.TIPO_MOVIMENTACAO_JUNTADA)
					m = CpMarcador.MARCADOR_JUNTADO;
				if (t == ExTipoMovimentacao.TIPO_MOVIMENTACAO_JUNTADA_EXTERNO)
					m = CpMarcador.MARCADOR_JUNTADO_EXTERNO;
				if (t == ExTipoMovimentacao.TIPO_MOVIMENTACAO_APENSACAO && apensadoAVolumeDoMesmoProcesso)
					m = CpMarcador.MARCADOR_APENSADO;
				if (t == ExTipoMovimentacao.TIPO_MOVIMENTACAO_TRANSFERENCIA_EXTERNA
						|| t == ExTipoMovimentacao.TIPO_MOVIMENTACAO_DESPACHO_TRANSFERENCIA_EXTERNA)
					m = CpMarcador.MARCADOR_TRANSFERIDO_A_ORGAO_EXTERNO;
				if ((t == ExTipoMovimentacao.TIPO_MOVIMENTACAO_DESPACHO_TRANSFERENCIA
						|| t == ExTipoMovimentacao.TIPO_MOVIMENTACAO_TRANSFERENCIA) && !apensadoAVolumeDoMesmoProcesso)
					m = CpMarcador.MARCADOR_CAIXA_DE_ENTRADA;
				if (t == ExTipoMovimentacao.TIPO_MOVIMENTACAO_DESPACHO
						&& mob.doc().isEletronico()) {
					m = CpMarcador.MARCADOR_DESPACHO_PENDENTE_DE_ASSINATURA;
					for (ExMovimentacao movAss : mob.getExMovimentacaoSet()) {
						if (movAss.getExTipoMovimentacao().getIdTpMov() == ExTipoMovimentacao.TIPO_MOVIMENTACAO_ASSINATURA_DIGITAL_MOVIMENTACAO
								&& movAss.getExMovimentacaoRef().getIdMov() == mov
										.getIdMov()) {
							m = mAnterior;
						}
					}
				}

				if (t == ExTipoMovimentacao.TIPO_MOVIMENTACAO_CRIACAO
						|| t == ExTipoMovimentacao.TIPO_MOVIMENTACAO_RECEBIMENTO
						|| t == ExTipoMovimentacao.TIPO_MOVIMENTACAO_DESARQUIVAMENTO
						|| t == ExTipoMovimentacao.TIPO_MOVIMENTACAO_DESOBRESTAR
						|| t == ExTipoMovimentacao.TIPO_MOVIMENTACAO_ASSINATURA_DIGITAL_DOCUMENTO
						|| t == ExTipoMovimentacao.TIPO_MOVIMENTACAO_CANCELAMENTO_JUNTADA
						|| t == ExTipoMovimentacao.TIPO_MOVIMENTACAO_DESAPENSACAO)
					if (!mob.doc().isEletronico() 
							&& (mob.doc().isAssinado())
							|| (mob.doc().isEletronico() && mob
									.doc()
									.isAssinadoEletronicoPorTodosOsSignatarios())
							|| mob.doc().getExTipoDocumento().getIdTpDoc() == 2
							|| mob.doc().getExTipoDocumento().getIdTpDoc() == 3) {
						
						if(!apensadoAVolumeDoMesmoProcesso)
							m = CpMarcador.MARCADOR_EM_ANDAMENTO;
						
					} else if (mob.isApensado()) {
						m = CpMarcador.MARCADOR_APENSADO;
					} else {
						m = CpMarcador.MARCADOR_PENDENTE_DE_ASSINATURA;
					}

				if (t == ExTipoMovimentacao.TIPO_MOVIMENTACAO_ANEXACAO
						&& mob.doc().isEletronico()) {
					m = CpMarcador.MARCADOR_ANEXO_PENDENTE_DE_ASSINATURA;
					/*
					 * n�o � poss�vel usar ExMovimentacao.isAssinada() pois n�o
					 * h� tempo habil no BD de efetivar a inclusao de
					 * movimentacao de assinatura de moviment��o
					 */
					for (ExMovimentacao movAss : mob.getExMovimentacaoSet()) {
						if ((movAss.getExTipoMovimentacao().getIdTpMov() == ExTipoMovimentacao.TIPO_MOVIMENTACAO_ASSINATURA_DIGITAL_MOVIMENTACAO || movAss
								.getExTipoMovimentacao().getIdTpMov() == ExTipoMovimentacao.TIPO_MOVIMENTACAO_CONFERENCIA_COPIA_DOCUMENTO)
								&& movAss.getExMovimentacaoRef().getIdMov() == mov
										.getIdMov()) {
							m = mAnterior;
							break;
						}
					}
				}

				if (m != mAnterior) {
					dt = mov.getDtIniMov();
					mAnterior = m;
				}
			}

			if (m == CpMarcador.MARCADOR_PENDENTE_DE_ASSINATURA) {
				if (!mob.getDoc().isAssinadoSubscritor())
					acrescentarMarca(set, mob,
							CpMarcador.MARCADOR_COMO_SUBSCRITOR, dt, mob
									.getExDocumento().getSubscritor(), null);
			}

			if (m == CpMarcador.MARCADOR_CAIXA_DE_ENTRADA) {
				if (!mob.doc().isEletronico()) {
					m = CpMarcador.MARCADOR_A_RECEBER;
					acrescentarMarca(set, mob, CpMarcador.MARCADOR_EM_TRANSITO,
							dt, ultMovNaoCanc.getCadastrante(),
							ultMovNaoCanc.getLotaCadastrante());
				} else {
					if (ultMovNaoCanc.getExTipoMovimentacao().getIdTpMov() == ExTipoMovimentacao.TIPO_MOVIMENTACAO_DESPACHO_TRANSFERENCIA) {
						m = CpMarcador.MARCADOR_DESPACHO_PENDENTE_DE_ASSINATURA;
					} else {
						acrescentarMarca(set, mob,
								CpMarcador.MARCADOR_EM_TRANSITO_ELETRONICO, dt,
								ultMovNaoCanc.getCadastrante(),
								ultMovNaoCanc.getLotaCadastrante());
					}
				}
			}
			if (m == CpMarcador.MARCADOR_TRANSFERIDO_A_ORGAO_EXTERNO) {
				acrescentarMarca(set, mob, m, dt,
						ultMovNaoCanc.getCadastrante(),
						ultMovNaoCanc.getLotaCadastrante());
			} else if (m == CpMarcador.MARCADOR_DESPACHO_PENDENTE_DE_ASSINATURA) {
				if (ultMovNaoCanc.getCadastrante().getId() != ultMovNaoCanc
						.getSubscritor().getId()) {
					if (ultMovNaoCanc.getLotaCadastrante().getIdLotacao() != ultMovNaoCanc
							.getLotaSubscritor().getIdLotacao()) {
						acrescentarMarca(set, mob, m, dt,
								ultMovNaoCanc.getSubscritor(),
								ultMovNaoCanc.getLotaSubscritor());
					} else {
						acrescentarMarca(set, mob, m, dt,
								ultMovNaoCanc.getSubscritor(), null);
					}
				}
				acrescentarMarca(set, mob, m, dt,
						ultMovNaoCanc.getCadastrante(),
						ultMovNaoCanc.getLotaCadastrante());
			} else if (m == CpMarcador.MARCADOR_JUNTADO
					|| m == CpMarcador.MARCADOR_APENSADO) {
				acrescentarMarca(set, mob, m, dt, null, null);
			} else {
				acrescentarMarca(set, mob, m, dt, ultMovNaoCanc.getResp(),
						ultMovNaoCanc.getLotaResp());
			}
		}
		return set;
	}

	public void agendarPublicacaoBoletim(final DpPessoa cadastrante,
			final DpLotacao lotaCadastrante, final ExDocumento doc)
			throws AplicacaoException {

		try {
			if (!anexosAlternativosEstaoAssinados(doc))
				throw new AplicacaoException(
						"O documento "
								+ doc.getCodigo()
								+ " possui documentos filhos do tipo anexo que n�o est�o assinados. ");

			iniciarAlteracao();

			final ExMovimentacao mov = criarNovaMovimentacao(
					ExTipoMovimentacao.TIPO_MOVIMENTACAO_AGENDAMENTO_DE_PUBLICACAO_BOLETIM,
					cadastrante, lotaCadastrante, doc.getMobilGeral(), null,
					null, null, null, null, null);

			gravarMovimentacao(mov);

			// Grava o documento na tabela de boletim
			ExBoletimDoc boletim = new ExBoletimDoc();
			boletim.setExDocumento(mov.getExDocumento());

			ExDao.getInstance().gravar(boletim);

			concluirAlteracao(mov.getExDocumento());
		} catch (final Exception e) {
			cancelarAlteracao();
			throw new AplicacaoException(
					"Erro ao agendar publica��o no boletim.", 0, e);
		}
	}

	public void publicarBoletim(final DpPessoa cadastrante,
			final DpLotacao lotaCadastrante, final ExDocumento doc,
			final Date dtPubl) throws AplicacaoException {

		try {
			iniciarAlteracao();

			final ExMovimentacao mov = criarNovaMovimentacao(
					ExTipoMovimentacao.TIPO_MOVIMENTACAO_PUBLICACAO_BOLETIM,
					cadastrante, lotaCadastrante, doc.getMobilGeral(), dtPubl,
					null, null, null, null, null);

			mov.setDescrMov("Publicado em " + mov.getDtMovDDMMYY());

			gravarMovimentacao(mov);
			concluirAlteracao(mov.getExDocumento());

			for (ExDocumento ex : obterDocumentosBoletim(doc))
				notificarPublicacao(cadastrante, lotaCadastrante, ex,
						mov.getDtMov(), doc);
		} catch (final Exception e) {
			cancelarAlteracao();
			throw new AplicacaoException(
					"Erro ao atestar publica��o no boletim.", 0, e);
		}
	}

	public void notificarPublicacao(final DpPessoa cadastrante,
			final DpLotacao lotaCadastrante, final ExDocumento doc,
			final Date dtPubl, final ExDocumento boletim)
			throws AplicacaoException {

		try {
			iniciarAlteracao();
			final ExMovimentacao mov = criarNovaMovimentacao(
					ExTipoMovimentacao.TIPO_MOVIMENTACAO_NOTIFICACAO_PUBL_BI,
					cadastrante, lotaCadastrante, doc.getMobilGeral(), dtPubl,
					null, null, null, null, null);

			mov.setExMobilRef(boletim.getMobilGeral());

			gravarMovimentacao(mov);
			concluirAlteracao(mov.getExDocumento());
		} catch (final Exception e) {
			cancelarAlteracao();
			throw new AplicacaoException("Erro ao notificar publica��o.", 0, e);
		}
	}

	public void pedirPublicacao(final DpPessoa cadastrante,
			final DpLotacao lotaCadastrante, final ExMobil mob,
			final Date dtMov, final DpPessoa subscritor,
			final DpPessoa titular, final DpLotacao lotaTitular,
			final Date dtDispPublicacao, final String tipoMateria, final String lotPublicacao, final String descrPublicacao)
			throws AplicacaoException {

		try {
			// Verifica se o documento possui anexos alterantivos e se estes
			// anexos est�o assinado
			if (!anexosAlternativosEstaoAssinados(mob.getExDocumento()))
				throw new AplicacaoException(
						"O documento "
								+ mob.getExDocumento().getCodigo()
								+ " possui documentos filhos do tipo anexo que n�o est�o assinados. ");

			iniciarAlteracao();

			DatasPublicacaoDJE DJE = new DatasPublicacaoDJE(dtDispPublicacao);
			// Esse teste � feito para validar a data de disponibiliza��o e
			// pegar a data de publica��o correta.
			// Pois se a data de disponibiliza��o for sexta o sistema coloca a
			// data de publica��o para segunda.
			String sMensagem = DJE.validarDataDeDisponibilizacao(true);
			if (sMensagem != null)
				throw new AplicacaoException(sMensagem);

			if (DJE.sao17Horas())
				throw new AplicacaoException(
						"Excedido Hor�rio de Solicita��o. Tente novamente amanh� ou defina a disponibiliza��o para um dia depois do escolhido");

			final ExMovimentacao mov = criarNovaMovimentacao(
					ExTipoMovimentacao.TIPO_MOVIMENTACAO_PEDIDO_PUBLICACAO,
					cadastrante, lotaCadastrante, mob, dtMov, subscritor, null,
					titular, lotaTitular, null);

			mov.setDtDispPublicacao(dtDispPublicacao);
			mov.setDescrMov("Publica��o prevista para "
					+ new SimpleDateFormat("dd/MM/yy").format(DJE
							.getDataPublicacao()));
			mov.setCadernoPublicacaoDje(tipoMateria);
			
			mov.setConteudoBlobXML("boletimadm",
					PublicacaoDJEBL.gerarXMLPublicacao(mov, tipoMateria, lotPublicacao, descrPublicacao));

			gravarMovimentacao(mov);

			// Verifica se est� na base de teste
			String mensagemTeste = null;
			if (!SigaExProperties.isAmbienteProducao())
				mensagemTeste = SigaExProperties.getString("email.baseTeste");

			StringBuffer sb = new StringBuffer(
					"Foi feita uma solicita��o de remessa do documento "
							+ mob.getExDocumento().getCodigo()
							+ " para publica��o no DJE.\n ");

			if (mensagemTeste != null)
				sb.append("\n " + mensagemTeste + "\n");

			StringBuffer sbHtml = new StringBuffer(
					"<html><body><p>Foi feita uma solicita��o de remessa do documento "
							+ mob.getExDocumento().getCodigo()
							+ " para publica��o no DJE.</p> ");

			if (mensagemTeste != null)
				sbHtml.append("<p><b>" + mensagemTeste + "</b></p>");

			sbHtml.append("</body></html>");

			TreeSet<CpConfiguracao> atendentes = getConf().getListaPorTipo(
					CpTipoConfiguracao.TIPO_CONFIG_ATENDER_PEDIDO_PUBLICACAO);

			ArrayList<String> emailsAtendentes = new ArrayList<String>();
			Date hoje = new Date(); 
			for (CpConfiguracao cpConf : atendentes) {
				if (!cpConf.ativaNaData(hoje)) 
					   continue;			
				if (!(cpConf instanceof ExConfiguracao))
					continue;
				ExConfiguracao conf = (ExConfiguracao) cpConf;
				if (conf.getCpSituacaoConfiguracao().getIdSitConfiguracao() == ExSituacaoConfiguracao.SITUACAO_PODE) {
					if (conf.getDpPessoa() != null) {
						if (!emailsAtendentes.contains(conf.getDpPessoa()
								.getEmailPessoaAtual())) {
							emailsAtendentes.add(conf.getDpPessoa()
									.getEmailPessoaAtual());
						}
					} else if (conf.getLotacao() != null) {
						List<DpPessoa> pessoasLotacao = CpDao.getInstance()
								.pessoasPorLotacao(
										conf.getLotacao().getIdLotacao(),
										false, true);
						for (DpPessoa pessoa : pessoasLotacao) {
							if (!emailsAtendentes.contains(pessoa
									.getEmailPessoaAtual()))
								emailsAtendentes.add(pessoa.getEmailPessoaAtual());
						}
					}
				}
			}

			
			Correio.enviar(
					SigaBaseProperties
							.getString("servidor.smtp.usuario.remetente"),
					emailsAtendentes.toArray(new String[emailsAtendentes.size()]),
					"Nova solicita��o de publica��o DJE ("
							+ mov.getLotaCadastrante().getSiglaLotacao() + ") ",
					sb.toString(), sbHtml.toString()); 

			concluirAlteracao(mov.getExDocumento());
		} catch (final Exception e) {
			cancelarAlteracao();
			throw new AplicacaoException("Erro ao agendar publica��o.", 0, e);
		}
	}

	public void registrarDisponibilizacaoPublicacao(final ExMobil mob,
			final Date dtMov, final String pagPublicacao)
			throws AplicacaoException {

		try {
			iniciarAlteracao();

			final ExMovimentacao mov = criarNovaMovimentacao(
					ExTipoMovimentacao.TIPO_MOVIMENTACAO_DISPONIBILIZACAO,
					null, null, mob, dtMov, null, null, null, null, null);

			mov.setPagPublicacao(pagPublicacao);

			/*
			 * mov .setDescrMov("Documento disponibilizado no Di�rio em " + new
			 * SimpleDateFormat("dd/MM/yy").format(mov .getDtMov()) + ", p�g " +
			 * mov.getPagPublicacao() + ". Data de publica��o: data de
			 * disponibiliza��o + 1, conforme prev� art. 4�, par�grafo 3� da Lei
			 * 11419 / 2006 ");
			 */

			mov.setDescrMov("Documento disponibilizado no Di�rio");

			// mov.setDtEfetivaDispPublicacao(dtEfetivaDispPublicacao);

			gravarMovimentacao(mov);
			concluirAlteracao(mov.getExDocumento());
		} catch (final Exception e) {
			cancelarAlteracao();
			throw new AplicacaoException("Erro ao registrar disponibiliza��o.",
					0, e);
		}
	}

	public void remeterParaPublicacao(final DpPessoa cadastrante,
			final DpLotacao lotaCadastrante, final ExMobil mob,
			final Date dtMov, final DpPessoa subscritor,
			final DpPessoa titular, final DpLotacao lotaTitular,
			final Date dtDispPublicacao, final String tipoMateria, final String lotPublicacao, final String descrPublicacao)
			throws Exception {

		try {

			// Verifica se o documento possui anexos alterantivos e se estes
			// anexos est�o assinado
			if (!anexosAlternativosEstaoAssinados(mob.getExDocumento()))
				throw new AplicacaoException(
						"O documento "
								+ mob.getExDocumento().getCodigo()
								+ " possui documentos filhos do tipo anexo que n�o est�o assinados. ");

			iniciarAlteracao();

			final ExMovimentacao mov = criarNovaMovimentacao(
					ExTipoMovimentacao.TIPO_MOVIMENTACAO_AGENDAMENTO_DE_PUBLICACAO,
					cadastrante, lotaCadastrante, mob, dtMov, subscritor, null,
					titular, lotaTitular, null);

			mov.setDtDispPublicacao(dtDispPublicacao);
			mov.setCadernoPublicacaoDje(tipoMateria);

			// Calendar cal = new GregorianCalendar();
			// cal.setTime(dtDispPublicacao);
			// cal.add(Calendar.DAY_OF_MONTH, 1);
			// Date dtPublicacao = cal.getTime();

			// String nomeArq = "I-" + doc.getIdDoc();
			// Gera o arquivo zip composto de RTF e XML
			GeradorRTF gerador = new GeradorRTF();
			mov.setConteudoBlobRTF("boletim",
					gerador.geraRTFFOP(mob.getExDocumento()));
			mov.setConteudoBlobXML("boletimadm",
					PublicacaoDJEBL.gerarXMLPublicacao(mov, tipoMateria, lotPublicacao, descrPublicacao));

			// Verifica se o documento possui documentos filhos do tipo Anexo
			if (mob.getExDocumentoFilhoSet() != null) {

			}

			if (tipoMateria.equals("A"))
				mov.setNmArqMov("ADM.zip");
			else
				mov.setNmArqMov("JUD.zip");


			try {
				PublicacaoDJEBL.primeiroEnvio(mov);
			} catch (Throwable t) {
				throw new Exception(t.getMessage() + " -- "
						+ t.getCause().getMessage());
			} 

			// mov.setNumTRFPublicacao(numTRF);
			DatasPublicacaoDJE DJE = new DatasPublicacaoDJE(dtDispPublicacao);
			DJE.validarDataDeDisponibilizacao(true);
			mov.setDescrMov("Publica��o prevista para "
					+ new SimpleDateFormat("dd/MM/yy").format(DJE
							.getDataPublicacao()));

			gravarMovimentacao(mov);
			concluirAlteracao(mov.getExDocumento());
		} catch (final Exception e) {
			cancelarAlteracao();
			throw new Exception(e.getMessage());
		}
	}

	public void anexarArquivo(final DpPessoa cadastrante,
			final DpLotacao lotaCadastrante, final ExMobil mob,
			final Date dtMov, final DpPessoa subscritor, final String nmArqMov,
			final DpPessoa titular, final DpLotacao lotaTitular,
			final byte[] conteudo, final String tipoConteudo, String motivo)
			throws AplicacaoException {

		final ExMovimentacao mov;

		try {
			iniciarAlteracao();

			mov = criarNovaMovimentacao(
					ExTipoMovimentacao.TIPO_MOVIMENTACAO_ANEXACAO, cadastrante,
					lotaCadastrante, mob, dtMov, subscritor, null, titular,
					lotaTitular, null);

			mov.setNmArqMov(nmArqMov);
			mov.setConteudoTpMov(tipoConteudo);
			mov.setConteudoBlobMov2(conteudo);
			mov.setDescrMov(motivo);

			gravarMovimentacao(mov);
			concluirAlteracao(mov.getExDocumento());

		} catch (final Exception e) {
			cancelarAlteracao();
			throw new AplicacaoException("Erro ao anexar documento.", 0, e);
		}

		try {
			encerrarAutomatico(cadastrante, lotaCadastrante, mob, dtMov);
		} catch (Exception e) {
			// TODO: handle exception
		}

		alimentaFilaIndexacao(mob.getExDocumento(), true);
	}

	public void arquivarCorrente(DpPessoa cadastrante,
			final DpLotacao lotaCadastrante, ExMobil mob, Date dtMov,
			Date dtMovIni, DpPessoa subscritor) throws AplicacaoException {

		SortedSet<ExMobil> set = mob.getMobilETodosOsApensos();
		for (ExMobil m : set) {
			if (m.getExDocumento().getDtFechamento() == null)
				throw new AplicacaoException(
						"N�o � poss�vel arquivar um documento n�o finalizado");
		}

		Date dt = dtMovIni != null ? dtMovIni : dao().dt();
		try {
			iniciarAlteracao();

			for (ExMobil m : set) {
				final ExMovimentacao mov = criarNovaMovimentacao(
						ExTipoMovimentacao.TIPO_MOVIMENTACAO_ARQUIVAMENTO_CORRENTE,
						cadastrante, lotaCadastrante, m, dtMov, subscritor,
						null, null, null, dt);
				gravarMovimentacao(mov);
				concluirAlteracaoParcial(m);
			}
			concluirAlteracao(null);
		} catch (final Exception e) {
			cancelarAlteracao();
			throw new AplicacaoException("Erro ao arquivar documento.", 0, e);
		}
	}

	public void arquivarPermanente(DpPessoa cadastrante,
			final DpLotacao lotaCadastrante, ExMobil mob, Date dtMov,
			DpPessoa subscritor) throws AplicacaoException {
		SortedSet<ExMobil> set = mob.getMobilETodosOsApensos();
		for (ExMobil m : set) {
			if (m.getExDocumento().getDtFechamento() == null)
				throw new AplicacaoException(
						"N�o � poss�vel arquivar um documento n�o finalizado");
		}

		Date dt = dao().dt();
		try {
			iniciarAlteracao();

			for (ExMobil m : set) {
				final ExMovimentacao mov = criarNovaMovimentacao(
						ExTipoMovimentacao.TIPO_MOVIMENTACAO_ARQUIVAMENTO_PERMANENTE,
						cadastrante, lotaCadastrante, m, dtMov, subscritor,
						null, null, null, dt);
				gravarMovimentacao(mov);
				concluirAlteracaoParcial(m);
			}
			concluirAlteracao(null);
		} catch (final Exception e) {
			cancelarAlteracao();
			throw new AplicacaoException(
					"Erro ao arquivar permanentemente um documento.", 0, e);
		}
	}

	public void sobrestar(DpPessoa cadastrante,
			final DpLotacao lotaCadastrante, ExMobil mob, Date dtMov,
			Date dtMovIni, DpPessoa subscritor) throws AplicacaoException {

		SortedSet<ExMobil> set = mob.getMobilETodosOsApensos();
		for (ExMobil m : set) {
			if (m.getExDocumento().getDtFechamento() == null)
				throw new AplicacaoException(
						"N�o � poss�vel sobrestar um documento n�o finalizado");
		}

		Date dt = dtMovIni != null ? dtMovIni : dao().dt();
		try {
			iniciarAlteracao();

			for (ExMobil m : set) {
				final ExMovimentacao mov = criarNovaMovimentacao(
						ExTipoMovimentacao.TIPO_MOVIMENTACAO_SOBRESTAR,
						cadastrante, lotaCadastrante, m, dtMov, subscritor,
						null, null, null, dt);
				gravarMovimentacao(mov);
				concluirAlteracaoParcial(m);
			}
			concluirAlteracao(null);
		} catch (final Exception e) {
			cancelarAlteracao();
			throw new AplicacaoException("Erro ao sobrestar documento.", 0, e);
		}
	}

	public String assinarDocumento(final DpPessoa cadastrante,
			final DpLotacao lotaCadastrante, final ExDocumento doc,
			final Date dtMov, final byte[] pkcs7) throws AplicacaoException {
		String sNome;
		Long lCPF = null;

		if (doc.isCancelado())
			throw new AplicacaoException(
					"N�o � poss�vel assinar um documento cancelado.");

		boolean fPreviamenteAssinado = doc.isAssinado();

		if (!fPreviamenteAssinado) {
			try {
				processarComandosEmTag(doc, "pre_assinatura");
			} catch (AplicacaoException e) {
				throw e;
			} catch (Exception e) {
				throw new Error(e);
			}
		}

		final byte[] cms;
		try {
			final byte[] data = doc.getConteudoBlobPdf();

			CdService client = Service.getCdService();

			String s;

			// s = client.validarAssinatura(pkcs7, data, dao().dt(),
			// VALIDAR_LCR);
			// Service.throwExceptionIfError(s);
			//
			// if (BUSCAR_CARIMBO_DE_TEMPO) {
			cms = client.validarECompletarAssinatura(pkcs7, data, true, dtMov);
			sNome = client
					.validarAssinatura(cms, data, dao().dt(), VALIDAR_LCR);

			// cms = client
			// .converterPkcs7EmCMSComCertificadosLCRsECarimboDeTempo(pkcs7);
			// Service.throwExceptionIfError(cms);
			//
			// sNome = client.validarAssinaturaCMS(
			// MessageDigest.getInstance("SHA1").digest(data), SHA1,
			// cms, dao().dt());
			Service.throwExceptionIfError(sNome);

			String sCPF = client.recuperarCPF(cms);
			Service.throwExceptionIfError(sCPF);

			lCPF = Long.valueOf(sCPF);
			// } else {
			// sNome = s;
			// String sCPF = client.recuperarCPF(pkcs7);
			// Service.throwExceptionIfError(sCPF);
			// lCPF = Long.valueOf(sCPF);
			// }

			// writeB64File("c:/trabalhos/java/cd_teste_doc.b64", data);
			// writeB64File("c:/trabalhos/java/cd_teste_hash.b64",
			// MessageDigest
			// .getInstance("SHA1").digest(data));
			// writeB64File("c:/trabalhos/java/cd_teste_pkcs7.b64", pkcs7);
			// writeB64File("c:/trabalhos/java/cd_teste_cms.b64", cms);

		} catch (final Exception e) {
			throw new AplicacaoException("Erro na assinatura de um documento",
					0, e);
		}

		boolean fValido = false;
		Long lMatricula = null;

		DpPessoa usuarioDoToken = null;

		// Obtem a matricula do assinante
		try {
			if (sNome == null)
				throw new AplicacaoException(
						"N�o foi poss�vel acessar o nome do assinante");
			String sMatricula = sNome.split(":")[1];
			lMatricula = Long.valueOf(sMatricula);
		} catch (final Exception e) {
			// throw new AplicacaoException(
			// "N�o foi poss�vel obter a matr�cula do assinante", 0, e);
		}

		// Verifica se a matr�cula confere com o subscritor titular ou com um
		// cossignatario
		try {
			if (lMatricula != null) {
				if (doc.getSubscritor() != null
						&& lMatricula
								.equals(doc.getSubscritor().getMatricula())) {
					fValido = true;
					usuarioDoToken = doc.getSubscritor();
				}
				if (!fValido) {
					fValido = (lMatricula.equals(doc.getCadastrante()
							.getMatricula()))
							&& (doc.getExTipoDocumento().getIdTpDoc() == ExTipoDocumento.TIPO_DOCUMENTO_EXTERNO);
				}
				if (!fValido)
					for (ExMovimentacao m : doc.getMobilGeral()
							.getExMovimentacaoSet()) {
						if (m.getExTipoMovimentacao().getIdTpMov() == ExTipoMovimentacao.TIPO_MOVIMENTACAO_INCLUSAO_DE_COSIGNATARIO
								&& m.getExMovimentacaoCanceladora() == null
								&& lMatricula.equals(m.getSubscritor()
										.getMatricula())) {
							fValido = true;
							usuarioDoToken = m.getSubscritor();
							continue;
						}
					}
			}

			if (!fValido && lCPF != null) {
				if (doc.getSubscritor() != null
						&& lCPF.equals(doc.getSubscritor().getCpfPessoa())) {
					fValido = true;
					usuarioDoToken = doc.getSubscritor();
				}
				if (!fValido) {
					fValido = (lCPF.equals(doc.getCadastrante().getCpfPessoa()))
							&& (doc.getExTipoDocumento().getIdTpDoc() == ExTipoDocumento.TIPO_DOCUMENTO_EXTERNO);
				}
				if (!fValido)
					for (ExMovimentacao m : doc.getMobilGeral()
							.getExMovimentacaoSet()) {
						if (m.getExTipoMovimentacao().getIdTpMov() == ExTipoMovimentacao.TIPO_MOVIMENTACAO_INCLUSAO_DE_COSIGNATARIO
								&& m.getExMovimentacaoCanceladora() == null
								&& lCPF.equals(m.getSubscritor().getCpfPessoa())) {
							fValido = true;
							usuarioDoToken = m.getSubscritor();
							continue;
						}
					}
			}

			if (lMatricula == null && lCPF == null)
				throw new AplicacaoException(
						"N�o foi poss�vel recuperar nem a matr�cula nem o CPF do assinante");
			if (fValido == false)
				throw new AplicacaoException(
						"Assinante n�o � subscritor nem cossignatario");
		} catch (final Exception e) {
			throw new AplicacaoException(
					"S� � permitida a assinatura digital do subscritor e dos cossignat�rios do documento",
					0, e);
		}

		String s = null;
		try {
			iniciarAlteracao();

			if (usuarioDoToken != null && usuarioDoToken.equivale(cadastrante))
				usuarioDoToken = cadastrante;

			final ExMovimentacao mov = criarNovaMovimentacao(
					ExTipoMovimentacao.TIPO_MOVIMENTACAO_ASSINATURA_DIGITAL_DOCUMENTO,
					cadastrante, lotaCadastrante, doc.getMobilGeral(), dtMov,
					usuarioDoToken, null, null, null, null);

			// if (BUSCAR_CARIMBO_DE_TEMPO) {
			// mov.setConteudoTpMov(CdService.MIME_TYPE_CMS);
			mov.setConteudoBlobMov2(cms);
			// } else {
			mov.setConteudoTpMov(CdService.MIME_TYPE_PKCS7);
			// mov.setConteudoBlobMov2(pkcs7);
			// }

			mov.setDescrMov(sNome);

			gravarMovimentacao(mov);
			concluirAlteracao(mov.getExDocumento());

			// Verifica se o documento possui documento pai e faz a juntada
			// autom�tica. Caso o pai seja um volume de um processo, primeiro
			// verifica se o volume est� encerrado, se estiver procura o �ltimo
			// volume para juntar.

			if (doc.getExMobilPai() != null) {
				if (doc.getExMobilPai().getDoc().isProcesso()
						&& doc.getExMobilPai().isEncerrado()) {
					doc.setExMobilPai(doc.getExMobilPai().doc()
							.getUltimoVolume());
					gravar(cadastrante, lotaCadastrante, doc);
				}
				juntarAoDocumentoPai(cadastrante, lotaCadastrante, doc, dtMov,
						cadastrante, cadastrante, mov);
			}

			if (doc.getExMobilAutuado() != null) {
				juntarAoDocumentoAutuado(cadastrante, lotaCadastrante, doc,
						dtMov, cadastrante, cadastrante, mov);
			}

			if (!fPreviamenteAssinado && doc.isAssinado()) {
				processarComandosEmTag(doc, "assinatura");
			}

		} catch (final Exception e) {
			cancelarAlteracao();
			throw new AplicacaoException("Erro ao assinar documento.", 0, e);
		}

		alimentaFilaIndexacao(doc, true);

		return s;
	}

	/**
	 * @param doc
	 * @param tag
	 * @throws Exception
	 */
	public String processarComandosEmTag(final ExDocumento doc, String tag)
			throws Exception {
		String s = processarModelo(doc, null, tag, null, null);

		// retorna a resposta produzida pelo processamento da instrucao de
		// assinatura
		int start = s.indexOf("<!-- " + tag + " -->");
		int end = s.indexOf("<!-- /" + tag + " -->");
		if (start != -1 && end != -1) {
			s = s.substring(start, end);
			if (s.contains("{") && s.contains("}")) {
				s = s.substring(s.lastIndexOf("{") + 1, s.indexOf("}"));
			} else
				s = null;
		} else
			s = null;

		return s;
	}

	/**
	 * @param sFileName
	 * @param data
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	private void writeB64File(String sFileName, final byte[] data)
			throws FileNotFoundException, IOException {
		FileOutputStream fout2 = new FileOutputStream(sFileName);
		fout2.write(Base64.encode(data).getBytes());
		fout2.close();
	}

	public void alimentaFilaIndexacao(ExDocumento doc, boolean reindexar) {
		try {
			if (doc == null || (!doc.isIndexavel()))
				return;
			BufferedWriter out = new BufferedWriter(new FileWriter(
					SigaExProperties.getString("siga.lucene.index.path")
							+ "/siga-ex-lucene-index-fila/" + doc.getIdDoc()));
			out.close();
		} catch (IOException e) {
			//
		}
	}

	public void assinarMovimentacao(DpPessoa cadastrante,
			DpLotacao lotaCadastrante, ExMovimentacao movAlvo,
			final byte[] pkcs7, long tpMovAssinatura) throws AplicacaoException {

		if (movAlvo != null) {
			log.info("Assinando movimentacao: " + movAlvo.toString()
					+ " Id da movimenta��o: " + movAlvo.getIdMov());
		} else {
			log.warn("A movimentacao a ser assinada nao pode ser nula");
			throw new AplicacaoException(
					"N�o � poss�vel assinar uma movimenta��o cancelada.");
		}

		if (movAlvo.isCancelada()) {
			log.warn("A movimentacao a ser assinada est� cancelada");
			throw new AplicacaoException(
					"N�o � poss�vel assinar uma movimenta��o cancelada.");
		}

		String sNome;
		Long lCPF = null;

		final byte[] cms;
		try {
			final byte[] data = movAlvo.getConteudoBlobpdf();

			CdService client = Service.getCdService();

			String s;

			// s = client.validarAssinatura(pkcs7, data, dao().dt(),
			// VALIDAR_LCR);
			// Service.throwExceptionIfError(s);
			//
			// if (BUSCAR_CARIMBO_DE_TEMPO) {
			cms = client.validarECompletarAssinatura(pkcs7, data, true,

			dao().dt());

			sNome = client
					.validarAssinatura(cms, data, dao().dt(), VALIDAR_LCR);

			Service.throwExceptionIfError(sNome);

			String sCPF = client.recuperarCPF(cms);
			Service.throwExceptionIfError(sCPF);

			lCPF = Long.valueOf(sCPF);

			boolean fValido = false;
			Long lMatricula = null;

			try {
				if (sNome == null)
					throw new AplicacaoException(
							"N�o foi poss�vel acessar o nome do assinante");
				String sMatricula = sNome.split(":")[1];
				lMatricula = Long.valueOf(sMatricula.replace("-", ""));
			} catch (final Exception e) {
				// throw new AplicacaoException(
				// "N�o foi poss�vel obter a matr�cula do assinante", 0, e);
			}

			// Verifica se a matr�cula confere com o subscritor do Despacho ou
			// do
			// desentranhamento
			if (movAlvo.getIdTpMov() == ExTipoMovimentacao.TIPO_MOVIMENTACAO_DESPACHO
					|| movAlvo.getIdTpMov() == ExTipoMovimentacao.TIPO_MOVIMENTACAO_DESPACHO_TRANSFERENCIA
					|| movAlvo.getIdTpMov() == ExTipoMovimentacao.TIPO_MOVIMENTACAO_DESPACHO_TRANSFERENCIA_EXTERNA
					|| movAlvo.getIdTpMov() == ExTipoMovimentacao.TIPO_MOVIMENTACAO_CANCELAMENTO_JUNTADA) {

				try {
					if (lMatricula != null) {
						fValido = movAlvo.getSubscritor() != null
								&& lMatricula.equals(movAlvo.getSubscritor()
										.getMatricula());
					}

					if (!fValido && lCPF != null) {
						fValido = movAlvo.getSubscritor() != null
								&& lCPF.equals(movAlvo.getSubscritor()
										.getCpfPessoa());
					}

					if (lMatricula == null && lCPF == null) {
						log.warn("N�o foi poss�vel recuperar nem a matr�cula nem o CPF do assinante");
						throw new AplicacaoException(
								"N�o foi poss�vel recuperar nem a matr�cula nem o CPF do assinante");
					}

					if (fValido == false
							&& movAlvo.getIdTpMov() == ExTipoMovimentacao.TIPO_MOVIMENTACAO_CANCELAMENTO_JUNTADA) {
						log.warn("Assinante n�o � subscritor do desentranhamento");
						throw new AplicacaoException(
								"Assinante n�o � subscritor do desentranhamento");
					}

					if (fValido == false) {
						log.warn("Assinante n�o � subscritor do despacho");
						throw new AplicacaoException(
								"Assinante n�o � subscritor do despacho");
					}

				} catch (final Exception e) {
					if (fValido == false
							&& movAlvo.getIdTpMov() == ExTipoMovimentacao.TIPO_MOVIMENTACAO_CANCELAMENTO_JUNTADA) {
						log.warn("S� � permitida a assinatura digital do subscritor do desentranhamento");
						throw new AplicacaoException(
								"S� � permitida a assinatura digital do subscritor do desentranhamento",
								0, e);
					}
					log.warn("S� � permitida a assinatura digital do subscritor do despacho");
					throw new AplicacaoException(
							"S� � permitida a assinatura digital do subscritor do despacho",
							0, e);
				}
			}

			log.info("Iniciando altera��o da movimenta��o "
					+ movAlvo.toString() + " Id da movimenta��o: "
					+ movAlvo.getIdMov());
			iniciarAlteracao();

			// Nato: isso esta errado. Deveriamos estar recebendo o cadastrante
			// e sua lotacao.
			final ExMovimentacao mov = criarNovaMovimentacao(tpMovAssinatura,
					cadastrante, lotaCadastrante, movAlvo.getExMobil(), null,
					null, null, null, null, null);

			mov.setExMovimentacaoRef(movAlvo);

			// if (BUSCAR_CARIMBO_DE_TEMPO) {
			// mov.setConteudoTpMov(CdService.MIME_TYPE_CMS);
			mov.setConteudoBlobMov2(cms);
			// } else {
			mov.setConteudoTpMov(CdService.MIME_TYPE_PKCS7);
			// mov.setConteudoBlobMov2(pkcs7);
			// }

			mov.setDescrMov(sNome);

			gravarMovimentacao(mov);
			concluirAlteracao(mov.getExDocumento());
		} catch (final AplicacaoException e) {
			cancelarAlteracao();
			throw e;
		} catch (final Exception e) {
			log.error("Erro ao assinar movimenta��o.", e);
			cancelarAlteracao();
			throw new AplicacaoException("Erro ao assinar movimenta��o.", 0, e);
		}

		// Caso o documento seja eletr�nico e a movimenta��o foi de assinatura
		// envia email para o atendente.
		if (movAlvo.getExDocumento().isEletronico()
				&& movAlvo.getExTipoMovimentacao().getIdTpMov() == ExTipoMovimentacao.TIPO_MOVIMENTACAO_DESPACHO_TRANSFERENCIA) {
			try {
				log.info("Enviando email de transferencia para o atendente");
				emailDeTransfer�ncia(movAlvo.getResp(), movAlvo.getLotaResp(),
						movAlvo.getExDocumento().getSigla(), movAlvo
								.getExDocumento().getCodigo(), movAlvo
								.getExDocumento().getDescrDocumento());

			} catch (final Exception e) {
				log.warn("Ocorreu um erro ao enviar email de transferencia para o atendente");
				try {
					log.info("Enviando email de transferencia para o administrador");
					emailDeTransfer�nciaAdministrador(movAlvo.getExDocumento()
							.getSigla(), e);
				} catch (final Exception e1) {
					log.warn("Ocorreu um erro ao enviar email de transferencia para o administrador");
				}
				/*
				 * throw new AplicacaoException(
				 * "Erro ao enviar email de notifica��o de transfer�ncia.", 0,
				 * e);
				 */
			}
		}
	}

	public void atualizarPublicacao(final DpPessoa cadastrante,
			final DpLotacao lotaCadastrante, final ExDocumento doc,
			final Date dtMov, final String pagPublicacao,
			final Date dtDispPublicacao) throws AplicacaoException {

		try {
			iniciarAlteracao();

			final ExMovimentacao mov = criarNovaMovimentacao(
					ExTipoMovimentacao.TIPO_MOVIMENTACAO_DISPONIBILIZACAO,
					cadastrante, lotaCadastrante, doc.getMobilGeral(), dtMov,
					null, null, null, null, null);

			mov.setPagPublicacao(pagPublicacao);
			mov.setDtDispPublicacao(dtDispPublicacao);

			mov.setDescrMov("Disponibilizado na internet em "
					+ new SimpleDateFormat("dd/MM/yy").format(dtDispPublicacao)
					+ ", na p�gina " + pagPublicacao);

			gravarMovimentacao(mov);
			concluirAlteracao(mov.getExDocumento());
		} catch (final Exception e) {
			cancelarAlteracao();
			throw new AplicacaoException("Erro ao cancelar juntada.", 0, e);
		}

	}

	public void cancelarDocumento(DpPessoa cadastrante,
			final DpLotacao lotaCadastrante, ExDocumento doc) throws Exception {

		// As altera��es aqui devem ser feitas em reabrir.
		try {
			iniciarAlteracao();
			cancelarMovimentacoes(cadastrante, lotaCadastrante, doc);
			concluirAlteracao(doc);
		} catch (final Exception e) {
			cancelarAlteracao();
			throw new AplicacaoException("Erro ao reabrir o documento.", 0, e);
		}
	}

	private void cancelarMovimentacoes(DpPessoa cadastrante,
			final DpLotacao lotaCadastrante, ExDocumento doc) throws Exception {
		// Cancelar todas as cria��es
		//
		for (ExMobil mob : doc.getExMobilSet()) {
			while (true) {
				ExMovimentacao mov = mob.getUltimaMovimentacaoNaoCancelada();
				if (mov == null)
					break;
				cancelarMovimentacao(cadastrante, lotaCadastrante, mob);
			}
		}
	}

	public void cancelarJuntada(final DpPessoa cadastrante,
			final DpLotacao lotaCadastrante, final ExMobil mob,
			final Date dtMov, final DpPessoa subscritor,
			final DpPessoa titular, String textoMotivo)
			throws AplicacaoException {

		try {
			iniciarAlteracao();

			final ExMovimentacao mov = criarNovaMovimentacao(
					ExTipoMovimentacao.TIPO_MOVIMENTACAO_CANCELAMENTO_JUNTADA,
					cadastrante, lotaCadastrante, mob, dtMov, subscritor, null,
					titular, null, null);

			final ExMovimentacao ultMov = mob.getUltimaMovimentacao();

			mov.setDescrMov(textoMotivo);

			if (mov.getExNivelAcesso() == null)
				mov.setExNivelAcesso(ultMov.getExNivelAcesso());

			if (ultMov.getExTipoMovimentacao().getId() != ExTipoMovimentacao.TIPO_MOVIMENTACAO_JUNTADA_EXTERNO) {
				mov.setExMovimentacaoRef(mov.getExMobil()
						.getUltimaMovimentacao(
								ExTipoMovimentacao.TIPO_MOVIMENTACAO_JUNTADA));
				final ExMobil mobPai = mov.getExMovimentacaoRef()
						.getExMobilRef();
				final ExMovimentacao ultMovPai = mobPai.getUltimaMovimentacao();

				mov.setExMobilRef(mobPai);

				mov.setLotaResp(ultMovPai.getLotaResp());
				mov.setResp(ultMovPai.getResp());

				if (mobPai.isNumeracaoUnicaAutomatica()) {
					List<ExArquivoNumerado> ans = mov.getExMobil()
							.filtrarArquivosNumerados(null, true);
					armazenarCertidaoDeDesentranhamento(mov, mobPai, ans,
							textoMotivo);
				}
			} else {
				mov.setExMovimentacaoRef(mov
						.getExMobil()
						.getUltimaMovimentacao(
								ExTipoMovimentacao.TIPO_MOVIMENTACAO_JUNTADA_EXTERNO));
				mov.setResp(mob.getExDocumento().getTitular());
				mov.setLotaResp(mob.getExDocumento().getLotaTitular());
			}

			gravarMovimentacao(mov);
			concluirAlteracao(mov.getExDocumento());
		} catch (final Exception e) {
			cancelarAlteracao();
			throw new AplicacaoException("Erro ao cancelar juntada.", 0, e);
		}
	}

	/**
	 * Calcula o n�mero original de p�ginas do documento que est� sendo
	 * desentranhado, com base em um vetor de arquivos numerados. Depois, inclui
	 * essa informa��o no contexto do processador html para gerar a certid�o.
	 * Tamb�m armazena o n�mero original de p�ginas em campo espec�fico da
	 * movimenta��o de cancelamento ou desentranhamento.
	 * 
	 * @param mov
	 * @param mob
	 * @param ans
	 * @param textoMotivo
	 * @throws AplicacaoException
	 * @throws IOException
	 * @throws UnsupportedEncodingException
	 * @throws Exception
	 */
	private void armazenarCertidaoDeDesentranhamento(final ExMovimentacao mov,
			final ExMobil mob, List<ExArquivoNumerado> ans, String textoMotivo)
			throws AplicacaoException, IOException,
			UnsupportedEncodingException, Exception {
		if (ans == null || ans.size() == 0)
			throw new AplicacaoException(
					"N�o foi poss�vel obter a numera��o �nica autom�tica das p�ginas da movimenta��o a ser cancelada.");

		Integer paginaInicial = ans.get(0).getPaginaInicial();
		Integer paginaFinal = ans.get(ans.size() - 1).getPaginaFinal();

		mov.setNumPaginasOri(paginaFinal - paginaInicial + 1);
		criarCertidaoDeDesentranhamento(mov, mob, paginaInicial, paginaFinal,
				textoMotivo);
	}

	/**
	 * @param movCanceladora
	 * @param textoMotivo
	 * @throws IOException
	 * @throws UnsupportedEncodingException
	 * @throws Exception
	 */
	private void criarCertidaoDeDesentranhamento(
			final ExMovimentacao movCanceladora, ExMobil mob, int folhaInicial,
			int folhaFinal, String textoMotivo) throws IOException,
			UnsupportedEncodingException, Exception {
		Map<String, String> form = new TreeMap<String, String>();
		form.put("folhaInicial", Integer.toString(folhaInicial));
		form.put("folhaFinal", Integer.toString(folhaFinal));
		form.put("textoMotivo", textoMotivo);
		movCanceladora.setConteudoBlobForm(urlEncodedFormFromMap(form));

		// Gravar o Html //Nato
		final String strHtml = processarModelo(movCanceladora,
				"processar_modelo", null, null);
		movCanceladora.setConteudoBlobHtmlString(strHtml);

		// Gravar o Pdf
		final byte pdf[] = Documento.generatePdf(strHtml);
		movCanceladora.setConteudoBlobPdf(pdf);
		movCanceladora.setConteudoTpMov("application/zip");
	}

	/**
	 * Retorna o conte�do de um Map<String,String> na forma de um array de bytes
	 * formatado de acordo com os padr�es de Url Encoded Form e utilizando
	 * iso-8859-1 como charset.
	 * 
	 * @param map
	 * @return
	 * @throws IOException
	 * @throws UnsupportedEncodingException
	 */
	public static byte[] urlEncodedFormFromMap(Map<String, String> map)
			throws IOException, UnsupportedEncodingException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		for (String sKey : map.keySet()) {
			if (baos.size() != 0)
				baos.write("&".getBytes("iso-8859-1"));
			baos.write(sKey.getBytes("iso-8859-1"));
			baos.write('=');
			baos.write(URLEncoder.encode(map.get(sKey), "iso-8859-1")
					.getBytes());
		}
		byte[] baForm = baos.toByteArray();
		return baForm;
	}

	/**
	 * @param map
	 * @param form
	 */
	public static void mapFromUrlEncodedForm(Map map, final byte[] form) {
		if (form != null) {
			final String as[] = new String(form).split("&");
			for (final String s : as) {
				final String param[] = s.split("=");
				try {
					if (param.length == 2) {
						map.put(param[0],
								URLDecoder.decode(param[1], "iso-8859-1"));
					}
				} catch (final UnsupportedEncodingException e) {
				}
			}
		}
	}

	public void cancelarMovimentacao(final DpPessoa cadastrante,
			final DpLotacao lotaCadastrante, final ExMobil mob)
			throws Exception {
		try {
			boolean indexar = false;
			SortedSet<ExMobil> set = null;
			ExMovimentacao movACancelar = mob
					.getUltimaMovimentacaoNaoCancelada();
			switch ((int) (long) movACancelar.getExTipoMovimentacao()
					.getIdTpMov()) {
			case (int) ExTipoMovimentacao.TIPO_MOVIMENTACAO_ARQUIVAMENTO_CORRENTE:
			case (int) ExTipoMovimentacao.TIPO_MOVIMENTACAO_SOBRESTAR:
			case (int) ExTipoMovimentacao.TIPO_MOVIMENTACAO_ARQUIVAMENTO_INTERMEDIARIO:
			case (int) ExTipoMovimentacao.TIPO_MOVIMENTACAO_ARQUIVAMENTO_PERMANENTE:
			case (int) ExTipoMovimentacao.TIPO_MOVIMENTACAO_DESARQUIVAMENTO:
			case (int) ExTipoMovimentacao.TIPO_MOVIMENTACAO_DESOBRESTAR:
				set = mob.getMobilETodosOsApensos();
				break;
			case (int) ExTipoMovimentacao.TIPO_MOVIMENTACAO_TRANSFERENCIA:
			case (int) ExTipoMovimentacao.TIPO_MOVIMENTACAO_TRANSFERENCIA_EXTERNA:
			case (int) ExTipoMovimentacao.TIPO_MOVIMENTACAO_RECEBIMENTO:
				set = mob.getMobilETodosOsApensos();
				break;
			case (int) ExTipoMovimentacao.TIPO_MOVIMENTACAO_DESPACHO:
			case (int) ExTipoMovimentacao.TIPO_MOVIMENTACAO_DESPACHO_TRANSFERENCIA:
			case (int) ExTipoMovimentacao.TIPO_MOVIMENTACAO_DESPACHO_TRANSFERENCIA_EXTERNA:
				indexar = true;
				set = new TreeSet<ExMobil>();
				set.add(mob);
				break;
			case (int) ExTipoMovimentacao.TIPO_MOVIMENTACAO_REDEFINICAO_NIVEL_ACESSO:
				indexar = true;
				set = mob.getMobilETodosOsApensos();
				break;
			default:
				set = new TreeSet<ExMobil>();
				set.add(mob);
			}

			iniciarAlteracao();
			for (ExMobil m : set) {
				if (m.getUltimaMovimentacao().getExTipoMovimentacao()
						.getIdTpMov() == ExTipoMovimentacao.TIPO_MOVIMENTACAO_AGENDAMENTO_DE_PUBLICACAO_BOLETIM) {
					cancelarPedidoBI(m.doc());
				}

				final ExMovimentacao ultMov = m.getUltimaMovimentacao();
				final ExMovimentacao ultMovNaoCancelada = m
						.getUltimaMovimentacaoNaoCancelada(movACancelar);

				/*
				 * O correto seria a vari�vel abaixo guardar a movimenta��o
				 * anterior � movimenta��o acima. N�o necessariamente ser� a
				 * pen�ltima.
				 */
				final ExMovimentacao penultMovNaoCancelada = m
						.getPenultimaMovimentacaoNaoCancelada();

				final ExMovimentacao mov = criarNovaMovimentacao(
						ExTipoMovimentacao.TIPO_MOVIMENTACAO_CANCELAMENTO_DE_MOVIMENTACAO,
						cadastrante, lotaCadastrante, m, null, null, null,
						null, null, null);

				gravarMovimentacao(mov);

				mov.setExMovimentacaoRef(ultMovNaoCancelada);
				mov.setExNivelAcesso(ultMovNaoCancelada.getExNivelAcesso());

		
				if (ultMovNaoCancelada.getExTipoMovimentacao().getIdTpMov() == ExTipoMovimentacao.TIPO_MOVIMENTACAO_AGENDAMENTO_DE_PUBLICACAO)
					PublicacaoDJEBL.cancelarRemessaPublicacao(mov);

				if (ultMovNaoCancelada.getExTipoMovimentacao().getIdTpMov() == ExTipoMovimentacao.TIPO_MOVIMENTACAO_CANCELAMENTO_JUNTADA
						&& ultMovNaoCancelada.getExMovimentacaoRef() != null) {
					mov.setExMobilRef(ultMovNaoCancelada.getExMovimentacaoRef()
							.getExMobilRef());
				}

				if (penultMovNaoCancelada != null) {
					// if (mov.getLotaResp() == null)
					mov.setLotaResp(penultMovNaoCancelada.getLotaResp());
					// if (mov.getResp() == null)
					mov.setResp(penultMovNaoCancelada.getResp());
					// if (mov.getLotaDestinoFinal() == null)
					mov.setLotaDestinoFinal(penultMovNaoCancelada
							.getLotaDestinoFinal());
					// if (mov.getDestinoFinal() == null)
					mov.setDestinoFinal(penultMovNaoCancelada.getDestinoFinal());
				} else {
					if (ultMovNaoCancelada != null) {
						// if (mov.getLotaResp() == null)
						mov.setLotaResp(ultMovNaoCancelada.getLotaResp());
						// if (mov.getResp() == null)
						mov.setResp(ultMovNaoCancelada.getResp());
						// if (mov.getLotaDestinoFinal() == null)
						mov.setLotaDestinoFinal(ultMovNaoCancelada
								.getLotaDestinoFinal());
						// if (mov.getDestinoFinal() == null)
						mov.setDestinoFinal(ultMovNaoCancelada
								.getDestinoFinal());
					} else {
						// if (mov.getLotaResp() == null)
						mov.setLotaResp(ultMov.getLotaResp());
						// if (mov.getResp() == null)
						mov.setResp(ultMov.getResp());
						// if (mov.getLotaDestinoFinal() == null)
						mov.setLotaDestinoFinal(ultMov.getLotaDestinoFinal());
						// if (mov.getDestinoFinal() == null)
						mov.setDestinoFinal(ultMov.getDestinoFinal());
					}
				}

				gravarMovimentacaoCancelamento(mov, ultMovNaoCancelada);
				concluirAlteracaoParcial(m);
			}

			concluirAlteracao(null);

			if (indexar)
				alimentaFilaIndexacao(mob.getExDocumento(), true);

		} catch (final Exception e) {
			cancelarAlteracao();
			throw new AplicacaoException("Erro ao cancelar movimenta��o.", 0, e);
			// throw e;
		}
	}

	public void cancelar(final DpPessoa titular, final DpLotacao lotaTitular,
			final ExMobil mob, final ExMovimentacao movCancelar,
			final Date dtMovForm, final DpPessoa subscritorForm,
			final DpPessoa titularForm, String textoMotivo) throws Exception {

		if (movCancelar.getIdTpMov() == ExTipoMovimentacao.TIPO_MOVIMENTACAO_ANEXACAO) {
			if (!getComp().podeCancelarAnexo(titular, lotaTitular, mob,
					movCancelar))
				throw new AplicacaoException("N�o � poss�vel cancelar anexo");
		} else if (movCancelar.getIdTpMov() == ExTipoMovimentacao.TIPO_MOVIMENTACAO_PEDIDO_PUBLICACAO) {
			if (!getComp().podeAtenderPedidoPublicacao(titular, lotaTitular,
					mob))
				throw new AplicacaoException(
						"Usu�rio n�o tem permiss�o de cancelar pedido de publica��o no DJE.");
		} else if (ExTipoMovimentacao.hasDespacho(movCancelar.getIdTpMov())) {
			if (!getComp().podeCancelarDespacho(titular, lotaTitular, mob,
					movCancelar))
				throw new AplicacaoException("N�o � poss�vel cancelar anexo");

		} else if (movCancelar.getIdTpMov() == ExTipoMovimentacao.TIPO_MOVIMENTACAO_VINCULACAO_PAPEL) {
			if (!Ex.getInstance()
					.getComp()
					.podeCancelarVinculacaoPapel(titular, lotaTitular, mob,
							movCancelar))
				throw new AplicacaoException(
						"N�o � poss�vel cancelar defini��o de perfil");

		} else if (movCancelar.getIdTpMov() == ExTipoMovimentacao.TIPO_MOVIMENTACAO_REFERENCIA) {
			if (!Ex.getInstance()
					.getComp()
					.podeCancelarVinculacaoDocumento(titular, lotaTitular, mob,
							movCancelar))
				throw new AplicacaoException(
						"N�o � poss�vel cancelar vincula��o de documento");

		} else if (movCancelar.getIdTpMov() != ExTipoMovimentacao.TIPO_MOVIMENTACAO_AGENDAMENTO_DE_PUBLICACAO_BOLETIM) {
			if (!getComp().podeCancelar(titular, lotaTitular, mob, movCancelar))
				throw new AplicacaoException(
						"N�o � permitido cancelar esta movimenta��o.");
		}

		try {
			iniciarAlteracao();
			final ExMovimentacao mov = criarNovaMovimentacao(
					ExTipoMovimentacao.TIPO_MOVIMENTACAO_CANCELAMENTO_DE_MOVIMENTACAO,
					titular, lotaTitular, mob, dtMovForm, subscritorForm, null,
					titularForm, null, null);

			// gravarMovimentacao(mov);
			mov.setExMovimentacaoRef(movCancelar);
			mov.setDescrMov(textoMotivo);

			if ((ExTipoMovimentacao.hasDocumento(movCancelar.getIdTpMov()))
					&& movCancelar.getExMobil().isNumeracaoUnicaAutomatica()) {
				List<ExArquivoNumerado> ans = mob.filtrarArquivosNumerados(
						mov.getExMovimentacaoRef(), false);
				armazenarCertidaoDeDesentranhamento(mov, mob, ans, textoMotivo);
				// if (ans.size() != 1)
				// throw new AplicacaoException(
				// "N�o foi poss�vel obter a numera��o �nica autom�tica das
				// p�ginas da movimenta��o a ser cancelada.");
				//
				// criarCertidaoDeDesentranhamento(mov, mob, ans.get(0)
				// .getPaginaInicial(), ans.get(0).getPaginaFinal());
			}

			if (movCancelar.getIdTpMov() == ExTipoMovimentacao.TIPO_MOVIMENTACAO_AGENDAMENTO_DE_PUBLICACAO_BOLETIM) {
				cancelarPedidoBI(mob.doc());
			}

			gravarMovimentacaoCancelamento(mov, movCancelar);
			concluirAlteracao(mov.getExDocumento());

		} catch (final Exception e) {
			cancelarAlteracao();
			throw new AplicacaoException("Erro ao cancelar movimenta��o.", 0, e);
			// throw e;
		}
	}

	public void criarVia(final DpPessoa cadastrante,
			final DpLotacao lotaCadastrante, final ExDocumento doc)
			throws Exception {
		criarVia(cadastrante, lotaCadastrante, doc, null);
		return;
	}

	public void criarVia(final DpPessoa cadastrante,
			final DpLotacao lotaCadastrante, final ExDocumento doc,
			Integer numVia) throws Exception {
		try {
			iniciarAlteracao();

			ExMobil mob = new ExMobil();
			mob.setExTipoMobil(dao().consultar(ExTipoMobil.TIPO_MOBIL_VIA,
					ExTipoMobil.class, false));
			mob.setNumSequencia(numVia == null ? (int) dao()
					.obterProximoNumeroVia(doc) : numVia);
			mob.setExDocumento(doc);
			doc.getExMobilSet().add(mob);
			mob = dao().gravar(mob);

			final ExMovimentacao mov = criarNovaMovimentacao(
					ExTipoMovimentacao.TIPO_MOVIMENTACAO_CRIACAO, cadastrante,
					lotaCadastrante, mob, null, null, null, null, null, null);

			gravarMovimentacao(mov);
			concluirAlteracao(doc);
		} catch (final Exception e) {
			cancelarAlteracao();
			throw new AplicacaoException("Erro ao criar nova via.", 0, e);
			// throw e;
		}
	}

	private ExDao dao() {
		return ExDao.getInstance();
	}

	public void desarquivar(final DpPessoa cadastrante,
			final DpLotacao lotaCadastrante, final ExMobil mob,
			final Date dtMov, final DpPessoa subscritor)
			throws AplicacaoException {
		SortedSet<ExMobil> set = mob.getMobilETodosOsApensos();
		for (ExMobil m : set) {
			if (!m.isArquivado())
				throw new AplicacaoException(
						"N�o � poss�vel desarquivar um documento que n�o esteja arquivado");
		}

		Date dt = dao().dt();
		try {
			iniciarAlteracao();

			for (ExMobil m : set) {
				final ExMovimentacao mov = criarNovaMovimentacao(
						ExTipoMovimentacao.TIPO_MOVIMENTACAO_DESARQUIVAMENTO,
						cadastrante, lotaCadastrante, m, dtMov, subscritor,
						null, null, null, dt);
				gravarMovimentacao(mov);
				concluirAlteracaoParcial(m);
			}
			concluirAlteracao(null);
		} catch (final Exception e) {
			cancelarAlteracao();
			throw new AplicacaoException("Erro ao desarquivar.", 0, e);
		}
	}

	public void desobrestar(final DpPessoa cadastrante,
			final DpLotacao lotaCadastrante, final ExMobil mob,
			final Date dtMov, final DpPessoa subscritor)
			throws AplicacaoException {
		SortedSet<ExMobil> set = mob.getMobilETodosOsApensos();
		for (ExMobil m : set) {
			if (!m.isSobrestado())
				throw new AplicacaoException(
						"N�o � poss�vel desobrestar um documento que n�o esteja sobrestado");
		}

		Date dt = dao().dt();
		try {
			iniciarAlteracao();

			for (ExMobil m : set) {
				final ExMovimentacao mov = criarNovaMovimentacao(
						ExTipoMovimentacao.TIPO_MOVIMENTACAO_DESOBRESTAR,
						cadastrante, lotaCadastrante, m, dtMov, subscritor,
						null, null, null, dt);
				gravarMovimentacao(mov);
				concluirAlteracaoParcial(m);
			}
			concluirAlteracao(null);
		} catch (final Exception e) {
			cancelarAlteracao();
			throw new AplicacaoException("Erro ao desobrestar.", 0, e);
		}
	}

	public void excluirMovimentacao(DpPessoa cadastrante,
			final DpLotacao lotaCadastrante, ExMobil mob, Long idMov)
			throws AplicacaoException {
		try {
			iniciarAlteracao();

			final ExMovimentacao mov = dao().consultar(idMov,
					ExMovimentacao.class, false);
			// ExTipoMovimentacao.TIPO_MOVIMENTACAO_ANEXACAO
			// movDao.excluir(mov);
			excluirMovimentacao(mov);
			if ((!mob.doc().isAssinado())
					&& (!mob.doc().isAssinadoDigitalmente()))
				processar(mob.getExDocumento(), true, false);
			concluirAlteracao(mov.getExDocumento());

		} catch (final Exception e) {
			cancelarAlteracao();
			throw new AplicacaoException("Erro ao excluir movimenta��o.", 0, e);
		}
		alimentaFilaIndexacao(mob.getExDocumento(), true);
	}

	@SuppressWarnings("unchecked")
	public String fechar(final DpPessoa cadastrante,
			final DpLotacao lotaCadastrante, ExDocumento doc) throws Exception {

		if (doc.getDtFechamento() != null)
			throw new AplicacaoException("Documento j� est� fechado.");
		
		Set<ExVia> setVias = doc.getSetVias();

		try {
			iniciarAlteracao();

			Date dt = dao().dt();
			Calendar c = Calendar.getInstance();
			c.setTime(dt);

			if (doc.getDtDoc() != null) {
				Calendar dtDocCalendar = Calendar.getInstance();
				dtDocCalendar.setTime(doc.getDtDoc());

				if (c.before(dtDocCalendar))
					throw new Exception(
							"N�o � permitido criar documento com data futura");
			}

			// Pega a data sem horas, minutos e segundos...
			if (doc.getDtDoc() == null) {
				c.set(c.get(Calendar.YEAR), c.get(Calendar.MONTH),
						c.get(Calendar.DAY_OF_MONTH));
				doc.setDtDoc(c.getTime());
			}

			if (doc.getOrgaoUsuario() == null)
				doc.setOrgaoUsuario(doc.getLotaCadastrante().getOrgaoUsuario());

			if (doc.getNumExpediente() == null)
				doc.setNumExpediente(obterProximoNumero(doc));

			doc.setDtFechamento(dt);

			if (doc.getExMobilPai() != null) {
				if (doc.getExMobilPai().doc().isProcesso() && doc.isProcesso()) {
					if (getComp().podeCriarSubprocesso(cadastrante,
							doc.getLotaCadastrante(), doc.getExMobilPai())) {
						int n = dao().obterProximoNumeroSubdocumento(
								doc.getExMobilPai());
						doc.setNumSequencia(n);
					} else {
						throw new AplicacaoException(
								"Documento filho n�o pode ser criado nessas condi��es.");
					}
				}
			}

			processar(doc, false, false);

			doc.setNumPaginas(doc.getContarNumeroDePaginas());
			dao().gravar(doc);

			if (doc.getExFormaDocumento().getExTipoFormaDoc().isExpediente()) {
				for (final ExVia via : setVias) {
					Integer numVia = null;
					if (via.getCodVia() != null)
						numVia = Integer.parseInt(via.getCodVia());
					if (numVia == null) {
						numVia = 1;
					}
					criarVia(cadastrante, lotaCadastrante, doc, numVia);
				}
			} else {
				criarVolume(cadastrante, lotaCadastrante, doc);
			}
			concluirAlteracao(doc);

			if (setVias == null || setVias.size() == 0)
				criarVia(cadastrante, lotaCadastrante, doc, null);

			String s = processarComandosEmTag(doc, "finalizacao");

			alimentaFilaIndexacao(doc, true);

			return s;
		} catch (final Exception e) {
			cancelarAlteracao();
			throw new AplicacaoException("Erro ao fechar documento: "
					+ e.getMessage(), 0, e);
		}
	}

	public Long obterProximoNumero(ExDocumento doc) throws Exception {
		doc.setAnoEmissao(Long.valueOf(new Date().getYear()) + 1900);

		Long num = dao().obterProximoNumero(doc, doc.getAnoEmissao());

		if (num == null) {
			// Verifica se reiniciar a numera��o ou continua com a numera��o
			// anterior
			if (getComp().podeReiniciarNumeracao(doc)) {
				num = 1L;
			} else {
				// Obt�m o pr�ximo n�mero considerando os anos anteriores at�
				// 2006
				Long anoEmissao = doc.getAnoEmissao();
				while (num == null && anoEmissao > 2005) {
					anoEmissao = anoEmissao - 1;
					num = dao().obterProximoNumero(doc, anoEmissao);
				}
				// Se continuar null � porque nunca foi criado documento para
				// este formato.
				if (num == null)
					num = 1L;
			}
		}

		return num;
	}

	public void criarVolume(DpPessoa cadastrante, DpLotacao lotaCadastrante,
			ExDocumento doc) throws AplicacaoException {
		try {
			iniciarAlteracao();

			ExMobil mob = new ExMobil();
			mob.setExTipoMobil(dao().consultar(ExTipoMobil.TIPO_MOBIL_VOLUME,
					ExTipoMobil.class, false));
			mob.setNumSequencia((int) dao().obterProximoNumeroVolume(doc));
			mob.setExDocumento(doc);
			doc.getExMobilSet().add(mob);
			mob = dao().gravar(mob);

			final ExMovimentacao mov = criarNovaMovimentacao(
					ExTipoMovimentacao.TIPO_MOVIMENTACAO_CRIACAO, cadastrante,
					lotaCadastrante, mob, null, null, null, null, null, null);

			gravarMovimentacao(mov);
			if (mob.getNumSequencia() > 1) {
				ExMobil mobApenso = mob.doc().getVolume(
						mob.getNumSequencia() - 1);
				ExMovimentacao movApenso = criarNovaMovimentacao(
						ExTipoMovimentacao.TIPO_MOVIMENTACAO_APENSACAO,
						cadastrante, lotaCadastrante, mobApenso, null, null,
						null, null, null, null);
				movApenso.setExMobilRef(mob);
				gravarMovimentacao(movApenso);
			}
			concluirAlteracao(doc);
		} catch (final Exception e) {
			cancelarAlteracao();
			throw new AplicacaoException("Erro ao criar novo volume.", 0, e);
			// throw e;
		}
	}

	/**
	 * @param cadastrante
	 * @param titular
	 * @param lotaTitular
	 * @param doc
	 * @param nomeProcesso
	 * @throws Exception
	 */
	public void criarWorkflow(final DpPessoa cadastrante, DpPessoa titular,
			final DpLotacao lotaTitular, ExDocumento doc, String nomeProcesso)
			throws Exception {
		{ // Utiliza o servi�o WebService do SigaWf
			WfService client = Service.getWfService();
			ArrayList<String> keys = new ArrayList<String>();
			ArrayList<String> values = new ArrayList<String>();
			{ // Adiciona no contexto a via Geral
				keys.add("doc_document");
				values.add(doc.getCodigo());
			}
			for (ExMobil mob : doc.getExMobilSet()) {
				if (mob.isGeral())
					continue;
				if (mob.isVia() || mob.isVolume()) {
					keys.add("doc_" + (char) ('a' + mob.getNumSequencia()));
					values.add((String) mob.getSigla());
				}
			}

			for (int n = 0; n < doc.getSetVias().size(); n++) { // Adiciona no
				// contexto a
				// via 'n'
				keys.add("doc_" + (char) ('a' + n));
				values.add(doc.getCodigo() + "-" + (char) ('A' + n));
			}
			List<ExPapel> papeis = dao().listarExPapeis();
			for (ExPapel papel : papeis) {
				List<DpResponsavel> responsaveis = doc
						.getResponsaveisPorPapel(papel);
				if (responsaveis != null && responsaveis.size() > 0) {
					keys.add("_doc_perfil_" + papel.getComoNomeDeVariavel());
					values.add(responsaveis.get(0).getSiglaCompleta());
				}
			}
			{
				client.criarInstanciaDeProcesso(
						nomeProcesso,
						SiglaParser.makeSigla(cadastrante,
								cadastrante.getLotacao()),
						SiglaParser.makeSigla(titular, lotaTitular), keys,
						values);
			}
		}
		atualizarWorkFlow(doc);
	}

	public static String descricaoConfidencial(ExDocumento doc,
			DpLotacao lotaTitular) {
		if (mostraDescricaoConfidencial(doc, lotaTitular))
			return "CONFIDENCIAL";
		else
			return doc.getDescrDocumento();
	}

	public String descricaoConfidencialDoDocumento(ExMobil mob,
			DpPessoa titular, DpLotacao lotaTitular) {

		try {
			if (!getComp().podeAcessarDocumento(titular, lotaTitular, mob))
				return "CONFIDENCIAL";
			else
				return mob.getExDocumento().getDescrDocumento();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			return "ERRO!";
		}
	}

	public static String selDescricaoConfidencial(Selecionavel sel,
			DpLotacao lotaTitular, DpPessoa titular) {
		if (sel instanceof ExDocumento) {
			ExDocumento doc = (ExDocumento) sel;
			if (mostraDescricaoConfidencial(doc, lotaTitular))
				return "CONFIDENCIAL";
			else
				return doc.getDescrDocumento();
		} else if (sel instanceof ExMobil) {
			ExDocumento doc = ((ExMobil) sel).getExDocumento();
			if (mostraDescricaoConfidencial(doc, lotaTitular))
				return "CONFIDENCIAL";
			else
				return doc.getDescrDocumento();
		} else if (sel instanceof DpLotacao) {
			DpLotacao lot = (DpLotacao) sel;
			return lot.getDescricaoAmpliada();
		}
		return sel.getDescricao();
	}

	public static boolean mostraDescricaoConfidencial(ExDocumento doc,
			DpLotacao lotaTitular) {
		if (doc == null)
			return false;
		if (doc.getExNivelAcesso() == null)
			return false;
		if (doc.getExNivelAcesso().getGrauNivelAcesso() > ExNivelAcesso.NIVEL_ACESSO_ENTRE_ORGAOS
				|| (doc.getExNivelAcesso().getGrauNivelAcesso() == ExNivelAcesso.NIVEL_ACESSO_ENTRE_ORGAOS && doc
						.getOrgaoUsuario().getIdOrgaoUsu() != lotaTitular
						.getOrgaoUsuario().getIdOrgaoUsu()))
			return true;
		return false;
	}

	public boolean mostraDescricaoConfidencialDoDocumento(ExDocumento doc,
			DpLotacao lotaTitular) {
		if (doc == null)
			return false;
		if (doc.getExNivelAcessoDoDocumento() == null)
			return false;
		if (doc.getExNivelAcessoDoDocumento().getGrauNivelAcesso() > ExNivelAcesso.NIVEL_ACESSO_ENTRE_ORGAOS
				|| (doc.getExNivelAcessoDoDocumento().getGrauNivelAcesso() == ExNivelAcesso.NIVEL_ACESSO_ENTRE_ORGAOS && doc
						.getOrgaoUsuario().getIdOrgaoUsu() != lotaTitular
						.getOrgaoUsuario().getIdOrgaoUsu()))
			return true;
		return false;
	}

	public static String anotacaoConfidencial(ExMobil mob, DpLotacao lotaTitular) {
		if (mostraDescricaoConfidencial(mob.doc(), lotaTitular))
			return "CONFIDENCIAL";
		else {
			String s = "";
			for (ExMovimentacao mov : mob.getExMovimentacaoSet()) {
				if (mov.isCancelada())
					continue;
				if (mov.getExTipoMovimentacao().getIdTpMov()
						.equals(ExTipoMovimentacao.TIPO_MOVIMENTACAO_ANOTACAO))
					s = mov.getDescrMov();
			}
			return s;
		}
	}

	public ExDocumento gravar(final DpPessoa cadastrante,
			final DpLotacao lotaCadastrante, ExDocumento doc) throws Exception {

		// Verifica se o documento possui documento pai e se o usu�rio possui
		// permiss�es de criar documento filho
		/*
		 * if (doc.getExMobilPai() != null &&
		 * doc.getExMobilPai().getExDocumento().getIdDoc() != null) if
		 * (doc.getExMobilPai().getExDocumento().isProcesso() &&
		 * doc.isProcesso()) { if
		 * (!ExCompetenciaBL.podeCriarSubprocesso(cadastrante, lotaCadastrante,
		 * doc.getExMobilPai())) throw new AplicacaoException(
		 * "N�o foi poss�vel criar Subprocesso do documento selecionado."); }
		 * else { if (!ExCompetenciaBL.podeCriarDocFilho(cadastrante,
		 * lotaCadastrante, doc.getExMobilPai())) throw new AplicacaoException(
		 * "N�o � permitido criar documento filho do documento pai selecionado, pois este est� inacess�vel ao usu�rio."
		 * ); }
		 */

		try {
			// System.out.println(System.currentTimeMillis() + " - INI gravar");
			iniciarAlteracao();

			if (doc.getCadastrante() == null)
				doc.setCadastrante(cadastrante);
			if (doc.getLotaCadastrante() == null) {
				doc.setLotaCadastrante(lotaCadastrante);
				if (doc.getLotaCadastrante() == null)
					doc.setLotaCadastrante(doc.getCadastrante().getLotacao());
			}
			if (doc.getDtRegDoc() == null)
				doc.setDtRegDoc(dao().dt());

			// Nato: para obter o numero do TMP na primeira grava��o
			if (doc.getIdDoc() == null)
				doc = ExDao.getInstance().gravar(doc);

			long tempoIni = System.currentTimeMillis();

			// a estrutura try catch abaixo foi colocada de modo a impedir que
			// os erros na formata��o impe�am a grava��o do documento
			try {
				processar(doc, false, false);
			} catch (Throwable t) {
				System.out.println("grava��o doc " + doc.getCodigo() + ", "
						+ new Date().toString() + " - erro na formata��o - "
						+ t.getMessage());
				t.printStackTrace();
			}

			System.out.println("monitorando gravacao IDDoc " + doc.getIdDoc()
					+ ", PESSOA " + doc.getCadastrante().getIdPessoa()
					+ ". Terminou processar: "
					+ (System.currentTimeMillis() - tempoIni));
			tempoIni = System.currentTimeMillis();

			if (doc.getConteudoBlobDoc() != null)
				doc.setConteudoTpDoc("application/zip");

			processarResumo(doc);

			doc.setNumPaginas(doc.getContarNumeroDePaginas());
			doc = ExDao.getInstance().gravar(doc);
			for (ExMobil mob : doc.getExMobilSet()) {
				if (mob.getIdMobil() == null)
					mob = ExDao.getInstance().gravar(mob);
			}

			if (doc.getMobilGeral() == null) {
				throw new AplicacaoException(
						"Documento precisa de mobil geral.");
			}

			String funcao = doc.getForm().get("acaoGravar");
			if (funcao != null) {
				obterMetodoPorString(funcao, doc);
			}

			concluirAlteracao(doc);

			System.out.println("monitorando gravacao IDDoc " + doc.getIdDoc()
					+ ", PESSOA " + doc.getCadastrante().getIdPessoa()
					+ ". Terminou commit gravacao: "
					+ (System.currentTimeMillis() - tempoIni));
			tempoIni = System.currentTimeMillis();

		} catch (final Exception e) {
			cancelarAlteracao();
			throw new AplicacaoException("Erro na grava��o", 0, e);

		}
		try {

		} catch (Exception ex) {
			//
		}
		// System.out.println(System.currentTimeMillis() + " - FIM gravar");
		return doc;
	}

	private void processarResumo(ExDocumento doc) throws Exception,
			UnsupportedEncodingException {
		String r = processarModelo(doc, null, "resumo", null, null);
		String resumo = null;
		if (r.contains("<!-- resumo -->") && r.contains("<!-- /resumo -->")) {
			if (r.contains("<!-- topico -->") && r.contains("<!-- /topico -->")) {
				r = r.substring(r.lastIndexOf("<!-- resumo -->") + 15,
						r.indexOf("<!-- /resumo -->")).trim();
				// r = r.replaceAll("\r", "");
				r = r.replaceAll("\n", "");
				r = r.replaceAll("\t", "");
				r = r.replaceAll("<!-- /topico -->", "");
				String topicos[] = r.split("<!-- topico -->");
				String topico = null;
				for (int i = 0; i < topicos.length; i++) {
					if (!topicos[i].equals("")) {
						String descr = topicos[i].substring(
								topicos[i].indexOf("name=\"")
										+ "name=\"".length(),
								topicos[i].indexOf("\" value="));
						String valor = topicos[i].substring(
								topicos[i].indexOf(" value=\"")
										+ " value=\"".length(),
								topicos[i].indexOf("\"/>"));
						topico = URLEncoder.encode(descr, "iso-8859-1") + "="
								+ URLEncoder.encode(valor, "iso-8859-1") + "&";
						if (resumo == null) {
							resumo = topico;
						} else {
							resumo = resumo + topico;
						}
					}
				}

				resumo = resumo.substring(0, resumo.length() - 1);

			}
		} else {
			resumo = null;
		}

		if (resumo != null) {
			doc.setConteudoBlobResumo(resumo.getBytes());
		}
	}

	public void bCorrigirDataFimMov(final ExMovimentacao mov) throws Exception {
		try {
			iniciarAlteracao();

			dao().gravar(mov);

			mov.getExMobil().getExMovimentacaoSet().add(mov);

			concluirAlteracao(mov.getExDocumento());

		} catch (final Exception e) {
			cancelarAlteracao();
			throw new AplicacaoException("Erro na grava��o", 0, e);

		}

	}

	public void bCorrigirCriacaoDupla(final ExMovimentacao mov)
			throws Exception {
		try {
			iniciarAlteracao();

			dao().excluir(mov);

			concluirAlteracao(mov.getExDocumento());

		} catch (final Exception e) {
			cancelarAlteracao();
			throw new AplicacaoException("Erro na grava��o", 0, e);

		}

	}

	private void gravarMovimentacao(final ExMovimentacao mov)
			throws AplicacaoException, SQLException {
		// if (mov.getNumVia() != null && mov.getNumVia() == 0)
		// mov.setNumVia(null);
		//
		// if (ultMov == null)
		// ultMov = mov.getExDocumento()
		// .getUltimaMovimentacao(mov.getNumVia());
		//
		// if (ultMov != null) {
		// ultMov.setDtFimMov(new Date());
		// ExDao.getInstance().gravar(ultMov);
		// }
		mov.setNumPaginas(mov.getContarNumeroDePaginas());
		dao().gravar(mov);

		/*
		 * if (mov.getConteudoBlobMov() != null) movDao.gravarConteudoBlob(mov);
		 */

		if (mov.getExMobil().getExMovimentacaoSet() == null)
			mov.getExMobil()
					.setExMovimentacaoSet(new TreeSet<ExMovimentacao>());

		mov.getExMobil().getExMovimentacaoSet().add(mov);

		if (!mov.getExTipoMovimentacao()
				.getIdTpMov()
				.equals(ExTipoMovimentacao.TIPO_MOVIMENTACAO_CANCELAMENTO_DE_MOVIMENTACAO)) {
			Notificador.notificarPerfisVinculados(mov,
					Notificador.TIPO_NOTIFICACAO_GRAVACAO);
		}
	}

	private void gravarMovimentacaoCancelamento(final ExMovimentacao mov,
			ExMovimentacao movCancelada) throws AplicacaoException,
			SQLException {
		// if (ultMov == null)
		// ultMov = mov.getExMobil().getUltimaMovimentacao();

		gravarMovimentacao(mov);

		if (movCancelada == null)
			movCancelada = mov.getExMobil().getUltimaMovimentacaoNaoCancelada();

		if (movCancelada != null) {
			movCancelada.setExMovimentacaoCanceladora(mov);
			dao().gravar(movCancelada);
		}

		Notificador.notificarPerfisVinculados(mov,
				Notificador.TIPO_NOTIFICACAO_CANCELAMENTO);
	}

	public void excluirMovimentacao(final ExMovimentacao mov)
			throws AplicacaoException, SQLException {

		// Date dtFim = mov.getDtFimMov();
		// Date dtIni = mov.getDtIniMov();
		//
		// mov.getExMobil().getExMovimentacaoSet().remove(mov);
		// for (ExMovimentacao m : mov.getExMobil().getExMovimentacaoSet()) {
		// if (m.getDtFimMov() != null && m.getDtFimMov().equals(dtIni)) {
		// ExMovimentacao ultMov = m;
		// ultMov.setDtFimMov(dtFim);
		// dao().gravar(ultMov);
		// break;
		// }
		// }
		dao().excluir(mov);
		mov.getExMobil().getExMovimentacaoSet().remove(mov);

		Notificador.notificarPerfisVinculados(mov,
				Notificador.TIPO_NOTIFICACAO_EXCLUSAO);
	}

	public void incluirCosignatario(final DpPessoa cadastrante,
			final DpLotacao lotaCadastrante, final ExDocumento doc,
			final Date dtMov, final DpPessoa subscritor,
			final String funcaoCosignatario) throws AplicacaoException {

		try {
			if (subscritor == null) {
				throw new AplicacaoException("Cossignat�rio n�o foi informado");
			}

			iniciarAlteracao();

			final ExMovimentacao mov = criarNovaMovimentacao(
					ExTipoMovimentacao.TIPO_MOVIMENTACAO_INCLUSAO_DE_COSIGNATARIO,
					cadastrante, lotaCadastrante, doc.getMobilGeral(), dtMov,
					subscritor, null, null, null, null);

			mov.setDescrMov(funcaoCosignatario);
			mov.setNmFuncaoSubscritor(funcaoCosignatario);

			gravarMovimentacao(mov);
			processar(doc, true, false);
			concluirAlteracao(doc);
		} catch (final Exception e) {
			cancelarAlteracao();
			throw new AplicacaoException("Erro ao incluir Cossignat�rio.", 0, e);
		}
	}

	public void juntarDocumento(final DpPessoa cadastrante,
			final DpPessoa docTitular, final DpLotacao lotaCadastrante,
			final String idDocExterno, final ExMobil mob, final ExMobil mobPai,
			final Date dtMov, final DpPessoa subscritor,
			final DpPessoa titular, final String idDocEscolha) throws Exception {

		if (idDocEscolha.equals("1")) {

			if (mobPai == null)
				throw new AplicacaoException(
						"N�o foi selecionado um documento para a juntada");

			if (mob.getExDocumento().getIdDoc()
					.equals(mobPai.getExDocumento().getIdDoc())
					&& mob.getNumSequencia().equals(mobPai.getNumSequencia())
					&& mob.getExTipoMobil().getIdTipoMobil()
							.equals(mobPai.getExTipoMobil().getIdTipoMobil()))
				throw new AplicacaoException(
						"N�o � poss�vel juntar um documento a ele mesmo");

			if (mobPai.getExDocumento().getDtFechamento() == null)
				throw new AplicacaoException(
						"N�o � poss�vel juntar a um documento n�o finalizado");

			if (mobPai.isGeral())
				throw new AplicacaoException(
						"� necess�rio informar a via � qual ser� feita a juntada");

			if (mob.doc().isEletronico()) {
				if (mob.temAnexosNaoAssinados()
						|| mob.temDespachosNaoAssinados())
					throw new AplicacaoException(
							"N�o � poss�vel juntar documento com anexo/despacho pendente de assinatura ou confer�ncia");
			}

			// Verifica se o documeto pai j� est� apensado a este documento
			for (ExMobil apenso : mob.getApensos()) {
				if (apenso.getId() == mobPai.getId())
					throw new AplicacaoException(
							"N�o � poss�vel juntar um documento a um documento que est� apensado a ele.");
			}

			if (!getComp().podeSerJuntado(docTitular, lotaCadastrante, mobPai))
				throw new AplicacaoException(
						"A via n�o pode ser juntada ao documento porque ele est� em tr�nsito, encerrado, juntado, cancelado, arquivado ou encontra-se em outra lota��o");
		}

		final ExMovimentacao mov;

		try {
			iniciarAlteracao();

			Long idTpMov;
			if (idDocEscolha.equals("1")) {
				idTpMov = ExTipoMovimentacao.TIPO_MOVIMENTACAO_JUNTADA;
			} else if (idDocEscolha.equals("2")) {
				idTpMov = ExTipoMovimentacao.TIPO_MOVIMENTACAO_JUNTADA_EXTERNO;
			} else
				throw new AplicacaoException("Op��o inv�lida.");

			mov = criarNovaMovimentacao(idTpMov, cadastrante, lotaCadastrante,
					mob, dtMov, subscritor, null, titular, null, null);

			mov.setExMobilRef(mobPai);

			if (idDocEscolha.equals("1")) {
				mov.setDescrMov("Juntado ao documento "
						+ mov.getExMobilRef().getCodigo().toString());
			} else if (idDocEscolha.equals("2")) {
				mov.setDescrMov(idDocExterno);
			} else
				throw new AplicacaoException("Op��o inv�lida.");

			if (idDocEscolha.equals("1") || idDocEscolha.equals("2")) {
			} else
				throw new AplicacaoException("Op��o inv�lida.");

			gravarMovimentacao(mov);
			concluirAlteracao(mov.getExDocumento());

		} catch (final Exception e) {
			cancelarAlteracao();
			throw new AplicacaoException("Erro ao juntar documento.", 0, e);
		}

		try {
			encerrarAutomatico(cadastrante, lotaCadastrante,
					mov.getExMobilRef(), dtMov);
		} catch (Exception e) {
			// TODO: handle exception
		}
	}

	public ExDocumento reabrir(DpPessoa cadastrante,
			final DpLotacao lotaCadastrante, ExDocumento doc) throws Exception {

		// As altera��es devem ser feitas em cancelardocumento.
		try {
			iniciarAlteracao();

			ExDocumento novoDoc = duplicarDocumento(cadastrante,
					lotaCadastrante, doc);

			cancelarMovimentacoes(cadastrante, lotaCadastrante, doc);

			concluirAlteracao(novoDoc);
			// atualizarWorkflow(doc, null);
			return novoDoc;
		} catch (final Exception e) {
			cancelarAlteracao();
			throw new AplicacaoException("Erro ao refazer o documento.", 0, e);
		}
	}

	// Nato: removi: HttpServletRequest request
	public ExDocumento duplicar(DpPessoa cadastrante,
			final DpLotacao lotaCadastrante, ExDocumento doc) throws Exception {

		try {
			iniciarAlteracao();

			ExDocumento novoDoc = duplicarDocumento(cadastrante,
					lotaCadastrante, doc);

			concluirAlteracao(novoDoc);
			return novoDoc;

		} catch (final Exception e) {
			cancelarAlteracao();
			throw new AplicacaoException("Erro ao duplicar o documento.", 0, e);
		}
	}

	private ExDocumento duplicarDocumento(DpPessoa cadastrante,
			final DpLotacao lotaCadastrante, ExDocumento doc) throws Exception {
		ExDocumento novoDoc = new ExDocumento();

		novoDoc.setConteudoBlobDoc(doc.getConteudoBlobDoc());
		novoDoc.setConteudoTpDoc(doc.getConteudoTpDoc());
		novoDoc.setDescrDocumento(doc.getDescrDocumento());

		if (doc.getDestinatario() != null && !doc.getDestinatario().isFechada())
			novoDoc.setDestinatario(doc.getDestinatario().getPessoaAtual());

		novoDoc.setFgEletronico(doc.getFgEletronico());
		novoDoc.setExNivelAcesso(doc.getExNivelAcesso());

		if (doc.getExClassificacao() != null
				&& !doc.getExClassificacao().isFechada())
			novoDoc.setExClassificacao(doc.getExClassificacao());

		novoDoc.setDescrClassifNovo(doc.getDescrClassifNovo());
		novoDoc.setExFormaDocumento(doc.getExFormaDocumento());
		
		if(!doc.getExModelo().isFechado())
			novoDoc.setExModelo(doc.getExModelo().getModeloAtual());
		else
			throw new AplicacaoException("N�o foi poss�vel duplicar o documento pois este modelo n�o est� mais em uso.");		
		
		novoDoc.setExTipoDocumento(doc.getExTipoDocumento());

		if (doc.getLotaDestinatario() != null
				&& !doc.getLotaDestinatario().isFechada())
			novoDoc.setLotaDestinatario(doc.getLotaDestinatario()
					.getLotacaoAtual());

		if (doc.getSubscritor() != null && !doc.getSubscritor().isFechada()) {
			novoDoc.setSubscritor(doc.getSubscritor().getPessoaAtual());
			novoDoc.setLotaSubscritor(doc.getSubscritor().getPessoaAtual()
					.getLotacao());
		}

		novoDoc.setNmArqDoc(doc.getNmArqDoc());
		novoDoc.setNmDestinatario(doc.getNmDestinatario());
		novoDoc.setNmFuncaoSubscritor(doc.getNmFuncaoSubscritor());
		novoDoc.setNmOrgaoExterno(doc.getNmOrgaoExterno());
		novoDoc.setNmSubscritorExt(doc.getNmSubscritorExt());
		novoDoc.setNumExtDoc(doc.getNumExtDoc());
		novoDoc.setObsOrgao(doc.getObsOrgao());
		novoDoc.setOrgaoExterno(doc.getOrgaoExterno());
		novoDoc.setOrgaoExternoDestinatario(doc.getOrgaoExternoDestinatario());
		novoDoc.setExMobilPai(doc.getExMobilPai());
		novoDoc.setOrgaoUsuario(doc.getOrgaoUsuario());

		if (doc.getTitular() != null && !doc.getTitular().isFechada())
			novoDoc.setTitular(doc.getTitular().getPessoaAtual());

		if (doc.getLotaTitular() != null && !doc.getLotaTitular().isFechada())
			novoDoc.setLotaTitular(doc.getLotaTitular().getLotacaoAtual());

		novoDoc.setNumAntigoDoc(doc.getNumAntigoDoc());
		novoDoc.setExDocAnterior(doc);
		// novoDoc.setNumPaginas(novoDoc.getContarNumeroDePaginas());

		ExMobil mob = new ExMobil();
		mob.setExTipoMobil(dao().consultar(ExTipoMobil.TIPO_MOBIL_GERAL,
				ExTipoMobil.class, false));
		mob.setNumSequencia(1);
		mob.setExDocumento(novoDoc);
		mob.setExMovimentacaoSet(new TreeSet<ExMovimentacao>());
		novoDoc.setExMobilSet(new TreeSet<ExMobil>());
		novoDoc.getExMobilSet().add(mob);

		novoDoc = gravar(cadastrante, lotaCadastrante, novoDoc);

		// mob = dao().gravar(mob);

		for (ExMovimentacao mov : doc.getMobilGeral().getExMovimentacaoSet()) {
			if (doc.getDtFechamento() != null
					&& mov.getDtIniMov().after(doc.getDtFechamento()))
				break;
			if (mov.isCancelada())
				continue;
			switch ((int) (long) mov.getIdTpMov()) {
			case (int) ExTipoMovimentacao.TIPO_MOVIMENTACAO_REDEFINICAO_NIVEL_ACESSO:
			case (int) ExTipoMovimentacao.TIPO_MOVIMENTACAO_INCLUSAO_DE_COSIGNATARIO:
			case (int) ExTipoMovimentacao.TIPO_MOVIMENTACAO_ANEXACAO:
			case (int) ExTipoMovimentacao.TIPO_MOVIMENTACAO_ANOTACAO:
			case (int) ExTipoMovimentacao.TIPO_MOVIMENTACAO_VINCULACAO_PAPEL:
				ExMovimentacao novaMov = duplicarMovimentacao(cadastrante,
						lotaCadastrante, mov);
				novaMov.setExMobil(novoDoc.getMobilGeral());

				try {
					iniciarAlteracao();
					gravarMovimentacao(novaMov);
					concluirAlteracao(novaMov.getExDocumento());
				} catch (final Exception e) {
					cancelarAlteracao();
					throw new AplicacaoException(
							"Erro ao gravar movimentacao.", 0, e);
				}
			}
		}

		// � necess�rio gravar novamente pois uma movimenta��o de inclus�o
		// de cossignat�rio pode ter sido introduzida, gerando a necessidade
		// de refazer o HTML e o PDF.
		novoDoc = gravar(cadastrante, lotaCadastrante, novoDoc);

		return novoDoc;
	}

	private ExMovimentacao duplicarMovimentacao(DpPessoa cadastrante,
			DpLotacao lotaCadastrante, ExMovimentacao mov)
			throws AplicacaoException {
		ExMovimentacao novaMov = new ExMovimentacao();
		novaMov.setCadastrante(cadastrante);
		novaMov.setConteudoBlobMov(mov.getConteudoBlobMov());
		novaMov.setConteudoTpMov(mov.getConteudoTpMov());
		novaMov.setDescrMov(mov.getDescrMov());
		novaMov.setDtIniMov(dao().dt());
		novaMov.setDtMov(mov.getDtMov());
		novaMov.setExNivelAcesso(mov.getExNivelAcesso());
		novaMov.setExTipoMovimentacao(mov.getExTipoMovimentacao());
		novaMov.setLotaCadastrante(lotaCadastrante);
		novaMov.setLotaResp(mov.getLotaResp());
		novaMov.setLotaSubscritor(mov.getLotaSubscritor());
		novaMov.setLotaTitular(mov.getLotaTitular());
		novaMov.setNmArqMov(mov.getNmArqMov());
		novaMov.setNmFuncaoSubscritor(mov.getNmFuncaoSubscritor());
		novaMov.setNumPaginas(mov.getNumPaginas());
		novaMov.setNumPaginasOri(mov.getNumPaginasOri());
		novaMov.setResp(mov.getResp());
		novaMov.setSubscritor(mov.getSubscritor());
		novaMov.setTitular(mov.getTitular());
		novaMov.setExPapel(mov.getExPapel());
		return novaMov;
	}

	// Nato: removi , final DpPessoa subscritor, final DpPessoa responsavel,
	// pois nao eram utilizados
	public void receber(final DpPessoa cadastrante,
			final DpLotacao lotaCadastrante, final ExMobil mob, final Date dtMov)
			throws AplicacaoException {
		
		if(mob.isEmTransitoExterno())
			return;
		
		SortedSet<ExMobil> set = mob.getMobilETodosOsApensos();

		try {
			iniciarAlteracao();

			for (ExMobil m : set) {
				final ExMovimentacao mov = criarNovaMovimentacao(
						ExTipoMovimentacao.TIPO_MOVIMENTACAO_RECEBIMENTO,
						cadastrante, lotaCadastrante, m, dtMov, cadastrante,
						null, null, null, null);

				final ExMovimentacao ultMov = m.getUltimaMovimentacao();
				if (ultMov.getDestinoFinal() != mov.getSubscritor()) {
					mov.setDestinoFinal(ultMov.getDestinoFinal());
					mov.setLotaDestinoFinal(ultMov.getLotaDestinoFinal());
				}

				gravarMovimentacao(mov);
				concluirAlteracaoParcial(m);
			}
			concluirAlteracao(null);
		} catch (final Exception e) {
			cancelarAlteracao();
			throw new AplicacaoException("Erro ao receber documento.", 0, e);
		}
	}

	public void receberEletronico(ExMovimentacao mov) throws AplicacaoException {

		// try {
		// iniciarAlteracao();
		// mov.setExEstadoDoc(dao().consultar(
		// ExEstadoDoc.ESTADO_DOC_EM_ANDAMENTO, ExEstadoDoc.class,
		// false));
		// dao().gravar(mov);
		// concluirAlteracao(mov.getExDocumento());
		// } catch (final Exception e) {
		// cancelarAlteracao();
		// throw new AplicacaoException("Erro ao receber documento.", 0, e);
		//
		// }

		// Nato: alterei para chamar a receber, pois temos que criar uma
		// movimentacao para influenciar no calculo dos marcadores
		receber(null, null, mov.getExMobil(), null);

	}

	public void referenciarDocumento(final DpPessoa cadastrante,
			final DpLotacao lotaCadastrante, final ExMobil mob,
			final ExMobil mobRef, final Date dtMov, final DpPessoa subscritor,
			final DpPessoa titular) throws AplicacaoException {

		if (mobRef == null)
			throw new AplicacaoException(
					"N�o foi selecionado um documento para o vinculo");

		if (mob.getExDocumento().getIdDoc()
				.equals(mobRef.getExDocumento().getIdDoc())
				&& mob.getNumSequencia().equals(mobRef.getNumSequencia())
				&& mob.getExTipoMobil().getIdTipoMobil()
						.equals(mobRef.getExTipoMobil().getIdTipoMobil()))
			throw new AplicacaoException(
					"N�o � poss�vel vincular um documento a ele mesmo");

		if (mobRef.getExDocumento().getDtFechamento() == null)
			throw new AplicacaoException(
					"N�o � poss�vel vincular-se a um documento n�o finalizado");

		try {
			iniciarAlteracao();

			final ExMovimentacao mov = criarNovaMovimentacao(
					ExTipoMovimentacao.TIPO_MOVIMENTACAO_REFERENCIA,
					cadastrante, lotaCadastrante, mob, dtMov, subscritor, null,
					titular, null, null);

			mov.setExMobilRef(mobRef);
			mov.setDescrMov("V�nculo: documento "
					+ mov.getExMobilRef().getCodigo().toString());

			gravarMovimentacao(mov);
			concluirAlteracao(mov.getExDocumento());
		} catch (final Exception e) {
			cancelarAlteracao();
			throw new AplicacaoException("Erro ao vincular documento.", 0, e);
		}
	}

	public String RegistrarAssinatura(final DpPessoa cadastrante,
			final DpLotacao lotaCadastrante, final ExDocumento doc,
			final Date dtMov, final DpPessoa subscritor, final DpPessoa titular)
			throws AplicacaoException {
		boolean fPreviamenteAssinado = doc.isAssinado();

		if (doc.getDtFechamento() == null)
			throw new AplicacaoException(
					"N�o � poss�vel registrar assinatura de um documento n�o finalizado");

		String s = null;
		try {
			iniciarAlteracao();

			final ExMovimentacao mov = criarNovaMovimentacao(
					ExTipoMovimentacao.TIPO_MOVIMENTACAO_REGISTRO_ASSINATURA_DOCUMENTO,
					cadastrante, lotaCadastrante, doc.getMobilGeral(), dtMov,
					subscritor, null, null, null, null);

			gravarMovimentacao(mov);
			concluirAlteracao(doc);

			// Verifica se o documento possui documento pai e faz a juntada
			// autom�tica.
			if (doc.getExMobilPai() != null) {
				juntarAoDocumentoPai(cadastrante, lotaCadastrante, doc, dtMov,
						subscritor, titular, mov);
			}

			if (!fPreviamenteAssinado && doc.isAssinado())
				s = processarComandosEmTag(doc, "assinatura");
		} catch (final Exception e) {
			cancelarAlteracao();
			throw new AplicacaoException("Erro ao registrar assinatura.", 0, e);
		}

		alimentaFilaIndexacao(doc, true);
		return s;
	}

	/**
	 * Transfere um documento de um local para outro. Esse m�todo trata tanto da
	 * movimenta��o "Despacho com transfer�ncia" como "Transfer�ncia".
	 * 
	 * @param orgaoExterno
	 * @param obsOrgao
	 * @param cadastrante
	 * @param lotaCadastrante
	 * @param mob
	 * @param dtMov
	 * @param dtMovIni
	 * @param lotaResponsavel
	 * @param responsavel
	 * @param lotaDestinoFinal
	 * @param destinoFinal
	 * @param subscritor
	 * @param titular
	 * @param tpDespacho
	 * @param fInterno
	 * @param descrMov
	 * @param conteudo
	 * @param nmFuncaoSubscritor
	 * @throws AplicacaoException
	 * @throws Exception
	 */
	// Nato: retirei: final HttpServletRequest request,

	public void transferir(final CpOrgao orgaoExterno, final String obsOrgao,
			final DpPessoa cadastrante, final DpLotacao lotaCadastrante,
			final ExMobil mob, final Date dtMov, final Date dtMovIni,
			DpLotacao lotaResponsavel, final DpPessoa responsavel,
			final DpLotacao lotaDestinoFinal, final DpPessoa destinoFinal,
			final DpPessoa subscritor, final DpPessoa titular,
			final ExTipoDespacho tpDespacho, final boolean fInterno,
			final String descrMov, final String conteudo,
			String nmFuncaoSubscritor) throws AplicacaoException, Exception {

		boolean fDespacho = tpDespacho != null || descrMov != null
				|| conteudo != null;

		boolean fTranferencia = lotaResponsavel != null || responsavel != null;

		SortedSet<ExMobil> set = mob.getMobilETodosOsApensos();

		Date dtUltReceb = null;

		if (fDespacho && mob.isVolumeApensadoAoProximo())
			throw new AplicacaoException(
					"N�o � poss�vel fazer despacho em um documento que fa�a parte de um apenso");

		if (fTranferencia && mob.doc().isEletronico()) {
			if (mob.doc().getMobilGeral().temAnexosNaoAssinados()
					|| mob.doc().getMobilGeral().temDespachosNaoAssinados())
				throw new AplicacaoException(
						"N�o � permitido fazer transfer�ncia em documento com anexo/despacho pendente de assinatura ou confer�ncia");

		}

		for (ExMobil m : set) {
			if (fDespacho && m.isEncerrado())
				if (m.isApensadoAVolumeDoMesmoProcesso())
					continue;
				else
					throw new AplicacaoException(
							"N�o � permitido fazer despacho em volume que esta encerrado");

			if (fDespacho
					&& !getComp()
							.podeDespachar(cadastrante, lotaCadastrante, m))
				throw new AplicacaoException("N�o � permitido fazer despacho.");

			if (fTranferencia) {

				if (lotaResponsavel.isFechada())
					throw new AplicacaoException(
							"N�o � permitido transferir documento para lota��o fechada");

				if (!getComp().podeTransferir(cadastrante, lotaCadastrante, m))
					throw new AplicacaoException("Transfer�ncia n�o permitida");
				if (!m.getExDocumento().isAssinado()
						&& !lotaResponsavel.equivale(m.getExDocumento()
								.getLotaTitular())
						&& !getComp().podeReceberDocumentoSemAssinatura(
								responsavel, lotaResponsavel, m))
					throw new AplicacaoException(
							"N�o � permitido fazer transfer�ncia em documento que ainda n�o foi assinado");

				if (m.doc().isEletronico()) {
					if (m.temAnexosNaoAssinados()
							|| m.temDespachosNaoAssinados())
						throw new AplicacaoException(
								"N�o � permitido fazer transfer�ncia em documento com anexo/despacho pendente de assinatura ou confer�ncia");

				}

				if (m.getExDocumento().isEletronico()
						&& !m.getExDocumento().jaTransferido()
						&& !m.getExDocumento()
								.isAssinadoEletronicoPorTodosOsSignatarios())
					throw new AplicacaoException(
							"N�o � permitido fazer transfer�ncia em documento que ainda n�o foi assinado por todos os subscritores.");

			}

			if (!fDespacho) {
				if (responsavel == null && lotaResponsavel == null)
					if (orgaoExterno == null && obsOrgao == null)
						throw new AplicacaoException(
								"N�o foram informados dados para o despacho/transfer�ncia");
			}

		}

		Date dt = dtMovIni != null ? dtMovIni : dao().dt();

		try {
			iniciarAlteracao();

			for (ExMobil m : set) {

				final ExMovimentacao mov;

				Long idTpMov;
				if (!fDespacho) {
					if (responsavel == null && lotaResponsavel == null)
						idTpMov = ExTipoMovimentacao.TIPO_MOVIMENTACAO_TRANSFERENCIA_EXTERNA;
					else
						idTpMov = ExTipoMovimentacao.TIPO_MOVIMENTACAO_TRANSFERENCIA;
				} else if (lotaResponsavel == null && responsavel == null
						&& orgaoExterno == null) {
					if (fInterno)
						idTpMov = ExTipoMovimentacao.TIPO_MOVIMENTACAO_DESPACHO_INTERNO;
					else
						idTpMov = ExTipoMovimentacao.TIPO_MOVIMENTACAO_DESPACHO;
				} else if (orgaoExterno != null) {
					if (fInterno)
						idTpMov = ExTipoMovimentacao.TIPO_MOVIMENTACAO_DESPACHO_TRANSFERENCIA_EXTERNA;
					else
						idTpMov = ExTipoMovimentacao.TIPO_MOVIMENTACAO_DESPACHO_TRANSFERENCIA_EXTERNA;
				} else {
					if (fInterno)
						idTpMov = ExTipoMovimentacao.TIPO_MOVIMENTACAO_DESPACHO_INTERNO_TRANSFERENCIA;
					else
						idTpMov = ExTipoMovimentacao.TIPO_MOVIMENTACAO_DESPACHO_TRANSFERENCIA;
				}

				// se for apensado e for despacho com transfer�ncia, interrompe
				// toda a transfer�ncia
				if (!m.equals(mob)
						&& (idTpMov == ExTipoMovimentacao.TIPO_MOVIMENTACAO_DESPACHO_TRANSFERENCIA_EXTERNA
								|| idTpMov == ExTipoMovimentacao.TIPO_MOVIMENTACAO_DESPACHO_INTERNO_TRANSFERENCIA || idTpMov == ExTipoMovimentacao.TIPO_MOVIMENTACAO_DESPACHO_TRANSFERENCIA)) {
					throw new AplicacaoException(
							"N�o � permitido fazer despacho com transfer�ncia em um documento que fa�a parte de um apenso. Fa�a primeiro o despacho e depois transfira o documento.");
				}
				// se n�o for apensado, pode.
				// se for apenas tranfer�ncia, pode.
				if (m.equals(mob)
						|| idTpMov == ExTipoMovimentacao.TIPO_MOVIMENTACAO_TRANSFERENCIA_EXTERNA
						|| idTpMov == ExTipoMovimentacao.TIPO_MOVIMENTACAO_TRANSFERENCIA) {

					final ExTipoMovimentacao tpmov = dao().consultar(idTpMov,
							ExTipoMovimentacao.class, false);

					mov = criarNovaMovimentacao(tpmov.getIdTpMov(),
							cadastrante, lotaCadastrante, m, dtMov,
							(subscritor == null && fDespacho) ? cadastrante
									: subscritor, null, titular, null, dt);

					if(dt != null)
						mov.setDtIniMov(dt);

					if (orgaoExterno != null || obsOrgao != null) {
						mov.setOrgaoExterno(orgaoExterno);
						mov.setObsOrgao(obsOrgao);
					}

					if (lotaResponsavel != null) {
						mov.setLotaResp(lotaResponsavel);
						mov.setResp(responsavel);
					} else {
						if (responsavel != null)
							lotaResponsavel = responsavel.getLotacao();
					}

					mov.setLotaTitular(mov.getLotaSubscritor());
					mov.setDestinoFinal(destinoFinal);
					mov.setLotaDestinoFinal(lotaDestinoFinal);

					mov.setNmFuncaoSubscritor(nmFuncaoSubscritor);

					mov.setExTipoDespacho(tpDespacho);
					mov.setDescrMov(descrMov);

					if (tpDespacho != null || descrMov != null
							|| conteudo != null) {
						// Gravar o form
						String cont = null;
						if (conteudo != null) {
							cont = conteudo;
						} else if (descrMov != null) {
							cont = descrMov;
						} else {
							cont = tpDespacho.getDescTpDespacho();
						}
						ByteArrayOutputStream baos = new ByteArrayOutputStream();
						baos.write("conteudo".getBytes("iso-8859-1"));
						baos.write('=');
						baos.write(URLEncoder.encode(cont, "iso-8859-1")
								.getBytes());
						mov.setConteudoBlobForm(baos.toByteArray());

						// Gravar o Html //Nato
						final String strHtml = processarModelo(mov,
								"processar_modelo", null, mov.getTitular()
										.getOrgaoUsuario());
						mov.setConteudoBlobHtmlString(strHtml);

						// Gravar o Pdf
						final byte pdf[] = Documento.generatePdf(strHtml);
						mov.setConteudoBlobPdf(pdf);
						mov.setConteudoTpMov("application/zip");
					}
					gravarMovimentacao(mov);
					concluirAlteracaoParcial(m);
				}
			}

			concluirAlteracao(null);

		} catch (final AplicacaoException e) {
			cancelarAlteracao();
			throw e;
		} catch (final Exception e) {
			cancelarAlteracao();
			throw new AplicacaoException("Erro ao transferir documento.", 0, e);
		}

		if (fDespacho) {
			try {
				encerrarAutomatico(cadastrante, lotaCadastrante, mob, dtMovIni);
			} catch (Exception e) {
				// TODO: handle exception
			}
		}

		// Inicio envio e-mail
		for (ExMobil m : set) {
			// Se o documento for eletr�nico e houve despacho, n�o envia
			// email
			// pois o despacho deve ser assinado primeiro

			if ((m.getExDocumento().isEletronico() && !fDespacho)
					|| getConf().podePorConfiguracao(responsavel,
							lotaResponsavel, m.doc().getExModelo(),
							CpTipoConfiguracao.TIPO_CONFIG_NOTIFICAR_POR_EMAIL)) {

				try {
					if (!fTranferencia)
						return;
					// Orlando: Inseri a condi��o abaixo para que o e-mail n�o
					// seja enviado quando tratar-se de despacho com
					// transfer�ncia que n�o estiver assinado.
					if (tpDespacho == null && descrMov == null && !m.isApensadoAVolumeDoMesmoProcesso())
						emailDeTransfer�ncia(responsavel, lotaResponsavel,
								m.getSigla(), m.getExDocumento()
										.getCodigoString(), m.getExDocumento()
										.getDescrDocumento());

				} catch (final Exception e) {
					try {
						emailDeTransfer�nciaAdministrador(m.getExDocumento()
								.getCodigoString(), e);
					} catch (final Exception e1) {

					}

					/*
					 * throw new AplicacaoException(
					 * "Erro ao enviar email de notifica��o de transfer�ncia." ,
					 * 0, e);
					 */
				}
			}
		}

	}

	private void emailDeTransfer�ncia(DpPessoa responsavel,

	DpLotacao lotaResponsavel, String sigla, String codigo,

	String descricao) throws Exception {

		String sDestinatario = null;

		ArrayList<String> dest = new ArrayList<String>();

		if (responsavel != null) {

			sDestinatario = responsavel.getNomePessoa();

			dest.add(responsavel.getEmailPessoaAtual());

		} else if (lotaResponsavel != null) {

			sDestinatario = lotaResponsavel.getNomeLotacao();

			List<String> listaDeEmails = dao().consultarEmailNotificacao(

			lotaResponsavel);

			// List<String> listaDeEmails = new ArrayList<String>();

			if (listaDeEmails.size() > 0) {

				for (String email : listaDeEmails) {

					// Caso exista alguma configura��o com email

					// nulo, significa que deve ser enviado para

					// todos da lota��o

					if (email == null) {

						for (DpPessoa pes : dao().pessoasPorLotacao(

						lotaResponsavel.getIdLotacao(), false, true)) {

							dest.add(pes.getEmailPessoaAtual());

						}

					} else {

						dest.add(email);

					}

				}

			} else {

				for (DpPessoa pes : dao().pessoasPorLotacao(

				lotaResponsavel.getIdLotacao(), false, true)) {

					dest.add(pes.getEmailPessoaAtual());

				}

			}

		}

		// S� envia email se possuir destinat�rios pois algumas lota��es n�o
		// possuem
		// servidores cadastrados e n�o possuem email cadastrados na 'tabela
		// siga.ex_email_notificacao'
		if (dest.size() > 0) { // Verifica se est� na base de teste
			String mensagemTeste = null;
			if (!SigaExProperties.isAmbienteProducao())
				mensagemTeste = SigaExProperties.getString("email.baseTeste");

			StringBuilder sb = new StringBuilder();

			sb.append("O documento ");

			sb.append(codigo);

			sb.append(", com descri��o '");

			sb.append(descricao);

			sb.append("', foi transferido para ");

			sb.append(sDestinatario);

			sb.append(" e aguarda recebimento.\n\n");

			sb.append("Para visualizar o documento, ");

			sb.append("clique no link abaixo:\n\n");

			sb.append("http://siga/sigaex/expediente/doc/exibir.action?sigla=");

			sb.append(sigla);

			if (mensagemTeste != null)
				sb.append("\n " + mensagemTeste + "\n");

			StringBuilder sbHTML = new StringBuilder();

			sbHTML.append("<html><body>");

			sbHTML.append("<p>O documento <b>");

			sbHTML.append(codigo);

			sbHTML.append("</b>, com descri��o '<b>");

			sbHTML.append(descricao);

			sbHTML.append("</b>', foi transferido para ");

			sbHTML.append(sDestinatario);

			sbHTML.append(" e aguarda recebimento.</p>");

			sbHTML.append("<p>Para visualizar o documento, ");

			sbHTML.append("clique <a href=\"");

			sbHTML.append("http://siga/sigaex/expediente/doc/exibir.action?sigla=");

			sbHTML.append(sigla);

			sbHTML.append("\">aqui</a>.</p>");

			if (mensagemTeste != null)
				sbHTML.append("<p><b>" + mensagemTeste + "</b></p>");

			sbHTML.append("</body></html>");

			Correio.enviar(SigaBaseProperties
					.getString("servidor.smtp.usuario.remetente"), dest

			.toArray(new String[dest.size()]),

			"Documento transferido para " + sDestinatario, sb.toString(),

			sbHTML.toString());
		}
	}

	private void emailDeTransfer�nciaAdministrador(String codigo,

	Exception e) throws Exception {

		ArrayList<String> dest = new ArrayList<String>();
		dest.add("fulano1@jfrj.jus.br");
		dest.add("fulano2@jfrj.jus.br");
		StringBuilder sb = new StringBuilder();

		String sMensagem = "";
		String sCausa = "";
		String sStackTrace = "";

		if (e.getMessage() != null)
			sMensagem = e.getMessage();

		if (e.getCause() != null && e.getCause().getMessage() != null)
			sCausa = e.getCause().getMessage();

		if (e.getStackTrace() != null) {
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			e.printStackTrace(pw);
			sStackTrace = sw.toString();
		}
		// Verifica se est� na base de teste
		String mensagemTeste = null;
		if (!SigaExProperties.isAmbienteProducao())
			mensagemTeste = SigaExProperties.getString("email.baseTeste");

		sb.append("Erro no documento ");

		sb.append(codigo);

		sb.append("\n\n");

		sb.append("Mensagem: " + sMensagem);

		sb.append("\n\n");

		sb.append("Causa: " + sCausa);

		if (mensagemTeste != null)
			sb.append("\n " + mensagemTeste + "\n");

		sb.append("\n\n");

		sb.append("StackTrace: " + sStackTrace);

		StringBuilder sbHTML = new StringBuilder();

		sbHTML.append("<html><body>");

		sbHTML.append("<p>Erro no documento  <b>");

		sbHTML.append(codigo);

		sbHTML.append("<br /><br />");

		sbHTML.append("Mensagem: " + sMensagem);

		sbHTML.append("<br /><br />");

		sbHTML.append("Causa: " + sCausa);

		if (mensagemTeste != null)
			sbHTML.append("<p><b>" + mensagemTeste + "</b></p>");

		sbHTML.append("<br /><br />");

		sbHTML.append("StackTrace: " + sStackTrace);

		sbHTML.append("</body></html>");

		Correio.enviar(
				SigaBaseProperties.getString("servidor.smtp.usuario.remetente"),
				dest.toArray(new String[dest.size()]), "Erro Siga-Doc",
				sb.toString(), sbHTML.toString());
	}

	public void registrarAcessoReservado(DpPessoa cadastrante,
			DpLotacao lotaCadastrante, ExMobil mob) throws AplicacaoException {

		try {
			iniciarAlteracao();

			final ExMovimentacao mov = criarNovaMovimentacao(
					ExTipoMovimentacao.TIPO_MOVIMENTACAO_REGISTRO_ACESSO_INDEVIDO,
					cadastrante, lotaCadastrante, mob, null, null, null, null,
					null, null);

			mov.setDescrMov("Visualizado por " + cadastrante.getNomePessoa()
					+ " (" + lotaCadastrante.getNomeLotacao() + ")");

			gravarMovimentacao(mov);
			concluirAlteracao(mov.getExDocumento());
		} catch (final Exception e) {
			cancelarAlteracao();
			throw new AplicacaoException("Erro ao fazer anota��o.", 0, e);
		}
	}

	// Nato: removi: final HttpServletRequest request,
	public void anotar(final DpPessoa cadastrante,
			final DpLotacao lotaCadastrante, final ExMobil mob,
			final Date dtMov, DpLotacao lotaResponsavel,
			final DpPessoa responsavel, final DpPessoa subscritor,
			final DpPessoa titular, final String descrMov,
			String nmFuncaoSubscritor) throws AplicacaoException {

		if (descrMov == null) {
			if (responsavel == null && lotaResponsavel == null)
				if (dtMov == null)
					throw new AplicacaoException(
							"N�o foram informados dados para a anota��o");
		}

		try {
			// criarWorkflow(cadastrante, lotaCadastrante, doc, "Exoneracao");
			iniciarAlteracao();

			final ExMovimentacao mov = criarNovaMovimentacao(
					ExTipoMovimentacao.TIPO_MOVIMENTACAO_ANOTACAO, cadastrante,
					lotaCadastrante, mob, dtMov, subscritor, null, titular,
					null, dtMov);

			mov.setNmFuncaoSubscritor(nmFuncaoSubscritor);
			mov.setDescrMov(descrMov);

			gravarMovimentacao(mov);
			concluirAlteracao(mov.getExDocumento());
		} catch (final Exception e) {
			cancelarAlteracao();
			throw new AplicacaoException("Erro ao fazer anota��o.", 0, e);
		}

		alimentaFilaIndexacao(mob.getExDocumento(), true);

	}

	// Nato: removi: final HttpServletRequest request,
	public void vincularPapel(final DpPessoa cadastrante,
			final DpLotacao lotaCadastrante, final ExMobil mob,
			final Date dtMov, DpLotacao lotaResponsavel,
			final DpPessoa responsavel, final DpPessoa subscritor,
			final DpPessoa titular, final String descrMov,
			String nmFuncaoSubscritor, ExPapel papel) throws AplicacaoException {

		if (descrMov == null) {
			if (responsavel == null && lotaResponsavel == null)
				if (dtMov == null)
					throw new AplicacaoException(
							"N�o foram informados dados para a vincula��o de papel");
		}
		if (papel == null)
			throw new AplicacaoException(
					"N�o foi informado o papel para a vincula��o de papel");

		try {
			// criarWorkflow(cadastrante, lotaCadastrante, doc, "Exoneracao");
			iniciarAlteracao();

			final ExMovimentacao mov = criarNovaMovimentacao(
					ExTipoMovimentacao.TIPO_MOVIMENTACAO_VINCULACAO_PAPEL,
					cadastrante, lotaCadastrante, mob, dtMov, responsavel,
					lotaResponsavel, titular, null, dtMov);

			mov.setNmFuncaoSubscritor(nmFuncaoSubscritor);
			mov.setDescrMov(descrMov);
			mov.setExPapel(papel);

			gravarMovimentacao(mov);
			concluirAlteracao(mov.getExDocumento());
		} catch (final Exception e) {
			cancelarAlteracao();
			throw new AplicacaoException("Erro ao fazer vincula��o de papel.",
					0, e);
		}

	}

	public void redefinirNivelAcesso(final DpPessoa cadastrante,
			final DpLotacao lotaCadastrante, final ExDocumento doc,
			final Date dtMov, DpLotacao lotaResponsavel,
			final DpPessoa responsavel, final DpPessoa subscritor,
			final DpPessoa titular, String nmFuncaoSubscritor,
			ExNivelAcesso nivelAcesso) throws AplicacaoException {

		if (nivelAcesso == null) {
			throw new AplicacaoException(
					"N�o foram informados dados para a redefini��o do n�vel de acesso");
		}

		if (nivelAcesso.getIdNivelAcesso().equals(
				doc.getExNivelAcesso().getIdNivelAcesso()))
			throw new AplicacaoException(
					"N�vel de acesso selecionado � igual ao atual");

		try {
			iniciarAlteracao();

			final ExMovimentacao mov = criarNovaMovimentacao(
					ExTipoMovimentacao.TIPO_MOVIMENTACAO_REDEFINICAO_NIVEL_ACESSO,
					cadastrante, lotaCadastrante, doc.getMobilGeral(), dtMov,
					subscritor, null, titular, null, dtMov);

			mov.setNmFuncaoSubscritor(nmFuncaoSubscritor);
			mov.setDescrMov("N�vel de acesso do documento alterado de "
					+ doc.getExNivelAcesso().getNmNivelAcesso() + " para "
					+ nivelAcesso.getNmNivelAcesso());

			mov.setExNivelAcesso(nivelAcesso);
			doc.setExNivelAcesso(nivelAcesso);

			gravarMovimentacao(mov);
			concluirAlteracao(mov.getExDocumento());
		} catch (final Exception e) {
			cancelarAlteracao();
			throw new AplicacaoException("Erro ao fazer anota��o", 0, e);
		}
		alimentaFilaIndexacao(doc, true);
	}

	/* Daqui para frente n�o s�o movimentacoes, mas sim funcoes auxiliares */

	public String canonicalizarHtml(String s, boolean fRemoverEspacos,
			boolean fRemoverTagsDesconhecidos, boolean fIso8859,
			boolean fBodyOnly) {
		String sRet;
		try {
			sRet = (new ProcessadorHtml().canonicalizarHtml(s, fRemoverEspacos,
					fRemoverTagsDesconhecidos, fIso8859, fBodyOnly, false));
			return sRet;
		} catch (Exception e) {
			return s;
		}
	}

	public void processar(final ExDocumento doc, final boolean gravar,
			final boolean transacao) throws Exception {
		try {
			if (doc != null
					&& (doc.isAssinado() || doc.isAssinadoDigitalmente()))
				throw new AplicacaoException(
						"O documento n�o pode ser reprocessado, pois j� est� assinado");

			if ((doc.getExModelo() != null && ("template/freemarker".equals(doc
					.getExModelo().getConteudoTpBlob()) || doc.getExModelo()
					.getNmArqMod() != null))
					|| doc.getExTipoDocumento().getIdTpDoc() == 2) {
				if (doc.getConteudoBlobForm() != null) {
				}
				if (gravar && transacao) {
					iniciarAlteracao();
				}

				// Internos antigos devem usar sempre o modelo 778L
				Long backupID = null;
				if (doc.getExTipoDocumento().getIdTpDoc() == 2) {
					if (doc.getExModelo() != null)
						backupID = doc.getExModelo().getIdMod();
					doc.setExModelo(dao().consultarAtivoPorIdInicial(
							ExModelo.class,
							doc.isProcesso() ? SigaExProperties.getIdModPA()
									: SigaExProperties
											.getIdModInternoImportado()));
				}

				final String strHtml;
				try {
					strHtml = processarModelo(doc, null, "processar_modelo",
							null, null);
				} catch (Exception e) {
					throw new AplicacaoException(
							"Erro no processamento do modelo HTML.", 0, e);
				}

				// Restaurar o modelo do "Interno Antigo"
				if (doc.getExTipoDocumento().getIdTpDoc() == 2) {
					if (backupID != null) {
						doc.setExModelo(dao().consultar(backupID,
								ExModelo.class, false));
					} else {
						doc.setExModelo(null);
					}
				}

				doc.setConteudoBlobHtmlString(strHtml);

				final byte pdf[];
				AbstractConversorHTMLFactory fabricaConvHtml = AbstractConversorHTMLFactory
						.getInstance();
				ConversorHtml conversor = fabricaConvHtml.getConversor(
						getConf(), doc, strHtml);

				try {
					pdf = Documento.generatePdf(strHtml, conversor);
				} catch (Exception e) {
					throw new AplicacaoException(
							"Erro na gera��o do PDF. Por favor, verifique se existem recursos de formata��o n�o suportados. Para eliminar toda a formata��o do texto clique em voltar e depois, no editor, clique no bot�o de 'Selecionar Tudo' e depois no bot�o de 'Remover Formata�ao'.",
							0, e);
				}
				doc.setConteudoBlobPdf(pdf);

				if (gravar) {
					doc.setNumPaginas(doc.getContarNumeroDePaginas());
					dao().gravar(doc);
					if (transacao) {
						concluirAlteracao(doc);
					}
				}
			}
		} catch (final AplicacaoException e) {
			if (gravar && transacao) {
				cancelarAlteracao();
			}
			throw e;
		} catch (final Exception e) {
			if (gravar && transacao) {
				cancelarAlteracao();
			}
			throw new AplicacaoException("Erro na grava��o", 0, e);
		}
	}

	public String processarModelo(final ExDocumento doc, String acao,
			Map<String, String> formParams, CpOrgaoUsuario orgaoUsu)
			throws Exception {
		return processarModelo(doc, null, acao, formParams, orgaoUsu);
	}

	public String processarModelo(final ExMovimentacao mov, final String acao,
			Map<String, String> formParams, CpOrgaoUsuario orgaoUsu)
			throws Exception {
		return processarModelo(mov.getExDocumento(), mov, acao, formParams,
				orgaoUsu);
	}

	private String processarModelo(ExDocumento doc, ExMovimentacao mov,
			String acao, Map<String, String> formParams, CpOrgaoUsuario orgaoUsu)
			throws Exception {
		Map<String, Object> attrs = new TreeMap<String, Object>();
		Map<String, Object> params = new TreeMap<String, Object>();
		ProcessadorModelo p = getProcessadorModeloJsp();

		// System.out.println(System.currentTimeMillis() + " - INI
		// processarModelo");

		// Parsear o registro de dados da entrevista que esta urlencoded
		// e gravar cada um dos valores no mapa de parametros
		if (formParams != null) {
			attrs.putAll(formParams);
		} else {
			if (doc.getExModelo() != null) {
				final byte[] form;
				if (mov == null)
					form = doc.getConteudoBlob("doc.form");
				else
					form = mov.getConteudoBlob("doc.form");
				mapFromUrlEncodedForm(attrs, form);
			}
		}
		if (acao != null)
			attrs.put(acao, "1");
		attrs.put("doc", doc);
		// rw.setAttribute("modelo", doc.getExModelo());
		if (mov == null) {
			if (doc.getExModelo() != null) {
				attrs.put("nmArqMod", doc.getExModelo().getNmArqMod());
				if ("template/freemarker".equals(doc.getExModelo()
						.getConteudoTpBlob())) {
					attrs.put("nmMod", doc.getExModelo().getNmMod());
					attrs.put("template", new String(doc.getExModelo()
							.getConteudoBlobMod2(), "utf-8"));
					p = processadorModeloFreemarker;
				}
			}
		} else {
			attrs.put("mov", mov);
			attrs.put("mob", mov.getExMobil());
			if (mov.getExTipoMovimentacao() != null
					&& mov.getExTipoMovimentacao().getIdTpMov() == ExTipoMovimentacao.TIPO_MOVIMENTACAO_ENCERRAMENTO) {
				attrs.put("nmArqMod", "certidaoEncerramentoVolume.jsp");
			} else if (mov.getExTipoMovimentacao() != null
					&& (mov.getIdTpMov() == ExTipoMovimentacao.TIPO_MOVIMENTACAO_CANCELAMENTO_JUNTADA || (mov
							.getIdTpMov() == ExTipoMovimentacao.TIPO_MOVIMENTACAO_CANCELAMENTO_DE_MOVIMENTACAO)
							&& ExTipoMovimentacao.hasDocumento(mov
									.getExMovimentacaoRef().getIdTpMov()))) {
				attrs.put("nmArqMod", "certidaoDesentranhamento.jsp");
			} else {
				if (mov.getExTipoDespacho() != null) {
					attrs.put("despachoTexto", mov.getExTipoDespacho()
							.getDescTpDespacho());
				} else if (mov.getDescrMov() != null) {
					attrs.put("despachoTexto", mov.getDescrMov());
				} else if (attrs.get("conteudo") != null) {
					attrs.put("despachoTexto", attrs.get("conteudo"));
				}
				if (mov.getConteudoBlobHtmlString() != null) {
					attrs.put("despachoHtml", mov.getConteudoBlobHtmlString());
				}
				// attrs.put("nmArqMod", "despacho_mov.jsp");
				ExModelo m = dao().consultarExModelo(null,
						"Despacho Autom�tico");
				attrs.put("nmMod", m.getNmMod());
				attrs.put("template", new String(m.getConteudoBlobMod2(),
						"utf-8"));

				p = processadorModeloFreemarker;
			}
		}

		// Nato: alterei essas linhas para que os modelos possam conhecer o
		// cadastrante e o titular
		// attrs.put("lotaTitular", doc.getLotaTitular());
		attrs.put("cadastrante", doc.getCadastrante());
		attrs.put("lotaCadastrante", doc.getLotaCadastrante());
		attrs.put("titular", doc.getTitular());
		attrs.put("lotaTitular", doc.getLotaTitular());

		params.put("processar_modelo", "1");
		params.put("finalizacao", "1");
		if (doc != null && doc.getIdDoc() != null)
			params.put("idDoc", doc.getIdDoc().toString());
		if (mov != null && mov.getIdMov() != null) {
			params.put("id", mov.getIdMov().toString());
		}
		CpOrgaoUsuario ou = orgaoUsu;
		if (doc != null)
			ou = doc.getOrgaoUsuario();
		if (mov != null && mov.getResp() != null
				&& mov.getResp().getOrgaoUsuario() != null)
			ou = mov.getResp().getOrgaoUsuario();
		String s = p.processarModelo(ou, attrs, params);

		// System.out.println(System.currentTimeMillis() + " - FIM
		// processarModelo");
		return s;
	}

	private void juntarAoDocumentoPai(final DpPessoa cadastrante,
			final DpLotacao lotaCadastrante, final ExDocumento doc,
			final Date dtMov, final DpPessoa subscritor,
			final DpPessoa titular, final ExMovimentacao mov) throws Exception,
			AplicacaoException {

		// for (int numVia = 1; numVia <= doc.getNumUltimaViaNaoCancelada();
		// numVia++)
		for (final ExMobil mob : doc.getExMobilSet()) {

			ExMovimentacao ultMov = mob.getUltimaMovimentacaoNaoCancelada();
			if (getComp().podeJuntar(ultMov.getCadastrante(),
					ultMov.getLotaCadastrante(), mob)
					& getComp().podeSerJuntado(ultMov.getCadastrante(),
							ultMov.getLotaCadastrante(), doc.getExMobilPai())) {
				juntarDocumento(ultMov.getCadastrante(), ultMov.getTitular(),
						ultMov.getLotaCadastrante(), null, mob,
						doc.getExMobilPai(), dtMov, ultMov.getSubscritor(),
						ultMov.getTitular(), "1");
				break;
			}
		}
	}

	private void juntarAoDocumentoAutuado(final DpPessoa cadastrante,
			final DpLotacao lotaCadastrante, final ExDocumento doc,
			final Date dtMov, final DpPessoa subscritor,
			final DpPessoa titular, final ExMovimentacao mov) throws Exception,
			AplicacaoException {

		// for (int numVia = 1; numVia <= doc.getNumUltimaViaNaoCancelada();
		// numVia++)
		for (final ExMobil mob : doc.getExMobilSet()) {
			ExMovimentacao ultMov = mob.getUltimaMovimentacaoNaoCancelada();
			if (getComp().podeJuntar(ultMov.getCadastrante(),
					ultMov.getLotaCadastrante(), doc.getExMobilAutuado())
					& getComp().podeSerJuntado(ultMov.getCadastrante(),
							ultMov.getLotaCadastrante(), mob)) {
				juntarDocumento(ultMov.getCadastrante(), ultMov.getTitular(),
						ultMov.getLotaCadastrante(), null,
						doc.getExMobilAutuado(), mob, dtMov,
						ultMov.getSubscritor(), ultMov.getTitular(), "1");
				break;
			}
		}
	}

	private ExMovimentacao criarNovaMovimentacao(final long idtpmov,
			final DpPessoa cadastrante, final DpLotacao lotaCadastrante,
			final ExMobil mob, final Date dtMov, final DpPessoa subscritor,
			final DpLotacao lotaSubscritor, final DpPessoa titular,
			final DpLotacao lotaTitular, final Date dtOrNull)
			throws AplicacaoException {
		final ExMovimentacao mov;
		mov = new ExMovimentacao();

		final ExTipoMovimentacao tpmov = dao().consultar(idtpmov,
				ExTipoMovimentacao.class, false);

		final Date dt = dtOrNull == null ? dao().dt() : dtOrNull;

		mov.setCadastrante(cadastrante);
		mov.setLotaCadastrante(lotaCadastrante);
		if (lotaCadastrante == null && mov.getCadastrante() != null)
			mov.setLotaCadastrante(mov.getCadastrante().getLotacao());
		if (subscritor != null) {
			mov.setSubscritor(subscritor);
			mov.setLotaSubscritor(mov.getSubscritor().getLotacao());
		} else {
			if (idtpmov == ExTipoMovimentacao.TIPO_MOVIMENTACAO_VINCULACAO_PAPEL) {
				mov.setSubscritor(null); /* o perfil(respons�vel) � uma lota��o */
				mov.setLotaSubscritor(lotaSubscritor);
			} else {
				mov.setSubscritor(cadastrante);
				mov.setLotaSubscritor(mov.getLotaCadastrante());
			}
		}

		if (titular != null)
			mov.setTitular(titular);
		else
			mov.setTitular(subscritor);
		if (lotaTitular != null)
			mov.setLotaTitular(lotaTitular);
		else
			mov.setLotaTitular(mov.getLotaSubscritor());

		if (dtMov != null)
			mov.setDtMov(dtMov);
		else
			mov.setDtMov(dt);

		// A data de in�cio da movimenta��o sempre ser� a data do servidor, n�o
		// a data que o usu�rio digitou
		mov.setDtIniMov(dao().dt());
		mov.setExMobil(mob);
		mov.setExTipoMovimentacao(tpmov);

		final ExMovimentacao ultMov = mob.getUltimaMovimentacao();
		if (ultMov != null) {
			// if (mov.getExMobilPai() == null)
			// mov.setExMobilPai(ultMov.getExMobilPai());
			if (mov.getExNivelAcesso() == null)
				mov.setExNivelAcesso(ultMov.getExNivelAcesso());
			if (mov.getLotaResp() == null)
				mov.setLotaResp(ultMov.getLotaResp());
			if (mov.getResp() == null)
				mov.setResp(ultMov.getResp());
			if (mov.getLotaDestinoFinal() == null)
				mov.setLotaDestinoFinal(ultMov.getLotaDestinoFinal());
			if (mov.getDestinoFinal() == null)
				mov.setDestinoFinal(ultMov.getDestinoFinal());
		}
		if (ultMov == null
				|| idtpmov == ExTipoMovimentacao.TIPO_MOVIMENTACAO_RECEBIMENTO) {
			if (mov.getLotaResp() == null)
				mov.setLotaResp(lotaCadastrante);
			if (mov.getResp() == null)
				mov.setResp(cadastrante);
		}
		return mov;
	}

	private void iniciarAlteracao() throws AplicacaoException {
		ExDao.iniciarTransacao();
	}

	private void concluirAlteracaoParcial(ExMobil mob) {
		SortedSet<ExMobil> set = threadAlteracaoParcial.get();
		if (set == null) {
			threadAlteracaoParcial.set(new TreeSet<ExMobil>());
			set = threadAlteracaoParcial.get();
		}
		if (mob.doc() != null)
			atualizarMarcas(mob.doc());
		set.add(mob);
	}

	private void concluirAlteracao(ExDocumento doc) throws Exception {
		if (doc != null)
			atualizarMarcas(doc);
		ExDao.commitTransacao();
		// if (doc != null)
		// atualizarWorkflow(doc, null);

		SortedSet<ExMobil> set = threadAlteracaoParcial.get();
		if (set != null && set.size() > 0){
			for (ExMobil mob : set) {
				atualizarWorkflow(mob.doc(), null);
			}
			set.clear();
		}else{
			 if (doc != null){
				 atualizarWorkflow(doc, null);	 
			 }
			 

		}
	}

	private void cancelarAlteracao() throws AplicacaoException {
		ExDao.rollbackTransacao();
		SortedSet<ExMobil> set = threadAlteracaoParcial.get();
		if (set != null)
			set.clear();
	}

	// Esse m�todo deve ser chamado sempre que houver alteracao no documento.
	public void atualizarWorkflow(ExDocumento doc, ExMovimentacao mov)
			throws AplicacaoException {

		try {
			if (Contexto.resource("isWorkflowEnabled") != null
					&& Boolean.valueOf(String.valueOf(Contexto.resource("isWorkflowEnabled")))) {
				if (mov != null) {
					atualizarWorkFlow(mov);
				} else {
					atualizarWorkFlow(doc);
				}
			}
		} catch (NullPointerException nulpointer) {
			System.err.println("N�o existe Contexto no ambiente");
		} catch (RuntimeException e) {
		}
	}

	public void atualizarWorkFlow(ExDocumento doc) throws AplicacaoException {
		try {
			Service.getWfService().atualizarWorkflowsDeDocumento(
					doc.getCodigo());
		} catch (Exception ex) {
			throw new AplicacaoException(
					"Erro ao tentar atualizar estado do workflow", 0, ex);
		}
	}

	public void atualizarWorkFlow(ExMovimentacao mov) throws AplicacaoException {
		try {
			Service.getWfService().atualizarWorkflowsDeDocumento(
					mov.getExMobil().getSigla());
		} catch (Exception e) {
			throw new AplicacaoException(
					"Erro ao tentar atualizar estado do workflow", 0, e);
		}
	}

	public List<ExDocumento> obterDocumentosBoletim(Map<String, String> form) {
		final Pattern p = Pattern.compile("^doc_boletim?([0-9]{1,7})");
		long l;
		List<ExDocumento> list = new ArrayList<ExDocumento>();
		ExDocumento doqueRef = new ExDocumento();
		for (String chave : form.keySet()) {
			final Matcher m = p.matcher(chave);
			if (m.find()) {
				if (m.group(1) != null && form.get(chave).equals("Sim")) {
					l = new Long(m.group(1));
					doqueRef = dao().consultar(l, ExDocumento.class, false);
					list.add(doqueRef);
				}
			}
		}
		return list;
	}

	public List<ExDocumento> obterDocumentosBoletim(ExDocumento doque) {
		return obterDocumentosBoletim(doque.getForm());

	}

	public List<ExFormaDocumento> obterFormasDocumento(DpPessoa titular,
			DpLotacao lotaTitular, ExTipoDocumento tipoDoc,
			ExTipoFormaDoc tipoForma, boolean protegido, boolean despachando,
			boolean autuando)

	throws Exception {
		List<ExFormaDocumento> formasSet = new ArrayList<ExFormaDocumento>();
		ArrayList<ExFormaDocumento> formasFinal = new ArrayList<ExFormaDocumento>();
		// Por enquanto, os par�metros tipoForma e tipoDoc n�o podem ser
		// preenchidos simultaneamente. Melhorar isso.
		if (tipoDoc != null)
			formasSet = new ArrayList<ExFormaDocumento>(
					tipoDoc.getExFormaDocumentoSet());
		else if (tipoForma != null)
			formasSet = new ArrayList<ExFormaDocumento>(
					tipoForma.getExFormaDocSet());
		else
			formasSet = ExDao.getInstance().listarTodosOrdenarPorDescricao();
		if (despachando) {
			for (ExFormaDocumento forma : formasSet) {
				if (getConf().podePorConfiguracao(titular, lotaTitular, forma,
						CpTipoConfiguracao.TIPO_CONFIG_DESPACHAVEL))
					formasFinal.add(forma);
			}
			formasSet = formasFinal;
			formasFinal = new ArrayList<ExFormaDocumento>();
		}
		if (autuando) {
			for (ExFormaDocumento forma : formasSet) {
				if (getConf().podePorConfiguracao(titular, lotaTitular, forma,
						CpTipoConfiguracao.TIPO_CONFIG_AUTUAVEL))
					formasFinal.add(forma);
			}
			formasSet = formasFinal;
			formasFinal = new ArrayList<ExFormaDocumento>();
		}
		if (protegido)
			for (ExFormaDocumento forma : formasSet) {
				if (getConf().podePorConfiguracao(titular, lotaTitular, forma,
						CpTipoConfiguracao.TIPO_CONFIG_CRIAR))
					formasFinal.add(forma);
			}
		else
			formasFinal.addAll(formasSet);
		return formasFinal;
	}

	public List<ExModelo> obterListaModelos(ExFormaDocumento forma,
			boolean despachando, String headerValue, boolean protegido,
			DpPessoa titular, DpLotacao lotaTitular, boolean autuando)
			throws Exception {
		ArrayList<ExModelo> modeloSetFinal = new ArrayList<ExModelo>();
		ArrayList<ExModelo> provSet;
		if (forma != null)
			modeloSetFinal = new ArrayList<ExModelo>(forma.getExModeloSet());
		else
			modeloSetFinal = (ArrayList) dao()
					.listarTodosModelosOrdenarPorNome(null);
		if (despachando) {
			provSet = new ArrayList<ExModelo>();
			for (ExModelo mod : modeloSetFinal)
				if (getConf().podePorConfiguracao(titular, lotaTitular, mod,
						CpTipoConfiguracao.TIPO_CONFIG_DESPACHAVEL))
					provSet.add(mod);
			modeloSetFinal = provSet;
		}
		if (autuando) {
			provSet = new ArrayList<ExModelo>();
			for (ExModelo mod : modeloSetFinal)
				if (getConf().podePorConfiguracao(titular, lotaTitular, mod,
						CpTipoConfiguracao.TIPO_CONFIG_AUTUAVEL))
					provSet.add(mod);
			modeloSetFinal = provSet;
		}
		if (protegido) {
			provSet = new ArrayList<ExModelo>();
			for (ExModelo mod : modeloSetFinal) {
				if (getConf().podePorConfiguracao(titular, lotaTitular, mod,
						CpTipoConfiguracao.TIPO_CONFIG_CRIAR))
					provSet.add(mod);
			}
			modeloSetFinal = provSet;
		}
		if (headerValue != null && modeloSetFinal.size() > 1) {
			ExModelo mod = new ExModelo();
			mod.setIdMod(0L);
			mod.setHisIdIni(0L);
			mod.setNmMod("[" + headerValue + "]");
			modeloSetFinal.add(0, mod);
		}
		return modeloSetFinal;
	}

	private boolean anexosAlternativosEstaoAssinados(ExDocumento doc) {
		// Verifica se o documento possui anexos alterantivos e se estes anexos
		// est�o assinado
		for (ExMobil mob : doc.getExMobilSet()) {
			for (ExDocumento docFilho : mob.getExDocumentoFilhoSet()) {
				// Verifica se docFilho � do tipo anexo
				if (docFilho.getExFormaDocumento().getIdFormaDoc() == 55) {
					if (!docFilho.isCancelado() && !docFilho.isAssinado())
						return false;
				}
			}
		}
		return true;
	}

	public void gravarBI(ExDocumento doc) throws Exception {
		final List<ExDocumento> documentosPublicar = obterDocumentosBoletim(doc);
		final List<ExDocumento> documentosNaoPublicar = obterDocumentosNaoVaoBoletim(doc
				.getForm());

		ExBoletimDoc boletim;
		ExDao exDao = ExDao.getInstance();

		for (ExDocumento docPubl : documentosPublicar) {
			boletim = exDao.consultarBoletimPorDocumento(docPubl);
			boletim.setBoletim(doc);
			exDao.gravar(boletim);
		}

		for (ExDocumento docPubl : documentosNaoPublicar) {
			boletim = exDao.consultarBoletimPorDocumento(docPubl);
			if (boletim.getBoletim() != null
					&& boletim.getBoletim().getIdDoc() == doc.getIdDoc())
				boletim.setBoletim(null);
			exDao.gravar(boletim);
		}
	}

	public void excluirBI(ExDocumento doc) throws Exception {
		ExDao exDao = ExDao.getInstance();

		final List<ExBoletimDoc> documentosDoBoletim = exDao
				.consultarBoletimPorBoletim(doc);
		for (ExBoletimDoc docBol : documentosDoBoletim) {
			docBol.setBoletim(null);
			exDao.gravar(docBol);
		}
	}

	public void cancelarPedidoBI(ExDocumento doc) throws Exception {
		ExBoletimDoc boletim;
		ExDao exDao = ExDao.getInstance();

		boletim = exDao.consultarBoletimPorDocumento(doc);
		exDao.excluir(boletim);
	}

	public void refazerBI(ExDocumento doc) throws Exception {
		final List<ExDocumento> documentosPublicar = obterDocumentosBoletim(doc);

		ExBoletimDoc boletim;
		ExDao exDao = ExDao.getInstance();

		for (ExDocumento docPubl : documentosPublicar) {
			boletim = exDao.consultarBoletimPorDocumento(docPubl);
			boletim.setBoletim(doc);
			exDao.gravar(boletim);
		}
	}

	public void finalizarBI(ExDocumento doc) throws Exception {
		final List<ExDocumento> documentosPublicar = obterDocumentosBoletim(doc);
		ExDao exDao = ExDao.getInstance();

		for (ExDocumento exDoc : documentosPublicar) {
			ExBoletimDoc boletim = exDao.consultarBoletimPorDocumento(exDoc);
			if (boletim == null) {
				throw new AplicacaoException(
						"Foi cancelada a solicita��o de pedido de publica��o do documento "
								+ exDoc.getCodigo()
								+ ". Por favor queira retorna a tela de edi��o de documento para uma nova confer�ncia.");
			} else if (boletim.getBoletim() == null) {
				throw new AplicacaoException(
						"O documento "
								+ exDoc.getCodigo()
								+ " foi retirado da lista de documentos deste boletim"
								+ ". Por favor queira retorna a tela de edi��o de documento para uma nova confer�ncia.");
			} else if (boletim.getBoletim() != doc) {
				throw new AplicacaoException(
						"O documento "
								+ exDoc.getCodigo()
								+ " j� consta da lista de documentos do boletim "
								+ boletim.getBoletim().getCodigo()
								+ ". Por favor queira retorna a tela de edi��o de documento para uma nova confer�ncia.");

			}
		}
	}

	public void obterMetodoPorString(String metodo, ExDocumento doc)
			throws Exception {
		if (metodo != null) {
			final Class[] classes = new Class[] { ExDocumento.class };

			ExBL exBl = Ex.getInstance().getBL();
			final Method method = ExBL.class.getDeclaredMethod(metodo, classes);
			method.invoke(exBl, new Object[] { doc });
		}
	}

	public byte[] obterPdfPorNumeroAssinatura(String num) throws Exception {

		ExArquivo arq = buscarPorNumeroAssinatura(num);

		Documento documento = new Documento();

		if (arq instanceof ExDocumento)
			return documento.getDocumento(((ExDocumento) arq).getMobilGeral(),
					null);
		if (arq instanceof ExMovimentacao) {
			ExMovimentacao mov = (ExMovimentacao) arq;
			return documento.getDocumento(mov.getExMobil(), mov);
		}

		return null;

	}

	public ExArquivo buscarPorNumeroAssinatura(String num) throws Exception {
		Pattern p = Pattern
				.compile("([0-9]{1,10})(.[0-9]{1,10})?-([0-9]{1,4})");
		Matcher m = p.matcher(num);

		if (!m.matches())
			throw new AplicacaoException("N�mero inv�lido");

		Long idDoc = Long.parseLong(m.group(1));

		ExDocumento doc = ExDao.getInstance().consultar(idDoc,
				ExDocumento.class, false);

		if (doc == null)
			throw new AplicacaoException("Documento n�o encontrado");

		// Testa se o documento existe na base
		try {
			doc.getDescrCurta();
		} catch (ObjectNotFoundException e) {
			throw new AplicacaoException("Documento n�o encontrado", 0, e);
		}

		/*
		 * if (doc.getExNivelAcesso().getGrauNivelAcesso() > 20) throw new
		 * Exception("Documento sigiloso");
		 */

		int hash = Integer.parseInt(m.group(3));

		ExMovimentacao move = null;

		if (Math.abs(doc.getDescrCurta().hashCode() % 10000) == hash) {
			return doc;
		} else {
			for (ExMovimentacao mov : doc.getExMovimentacaoSet())
				if (Math.abs((doc.getDescrCurta() + mov.getIdMov()).hashCode() % 10000) == hash)
					move = mov;
			if (move == null)
				throw new AplicacaoException("N�mero inv�lido");

			return move;
		}

	}

	public List<ExDocumento> obterDocumentosNaoVaoBoletim(
			Map<String, String> form) {
		final Pattern p = Pattern.compile("^doc_boletim?([0-9]{1,7})");
		long l;
		List<ExDocumento> list = new ArrayList<ExDocumento>();
		ExDocumento doqueRef = new ExDocumento();
		for (String chave : form.keySet()) {
			final Matcher m = p.matcher(chave);
			if (m.find()) {
				if (m.group(1) != null && form.get(chave).equals("Nao")) {
					l = new Long(m.group(1));
					doqueRef = ExDao.getInstance().consultar(l,
							ExDocumento.class, false);
					list.add(doqueRef);
				}
			}
		}
		return list;
	}

	public void apensarDocumento(final DpPessoa cadastrante,
			final DpPessoa docTitular, final DpLotacao lotaCadastrante,
			final ExMobil mob, final ExMobil mobMestre, final Date dtMov,
			final DpPessoa subscritor, final DpPessoa titular) throws Exception {

		if (mobMestre == null)
			throw new AplicacaoException(
					"N�o foi selecionado um documento para a apensa��o");

		if (mob.getExDocumento().getIdDoc()
				.equals(mobMestre.getExDocumento().getIdDoc())
				&& mob.getNumSequencia().equals(mobMestre.getNumSequencia())
				&& mob.getExTipoMobil().getIdTipoMobil()
						.equals(mobMestre.getExTipoMobil().getIdTipoMobil()))
			throw new AplicacaoException(
					"N�o � poss�vel apensar um documento a ele mesmo");

		if (mobMestre.getExDocumento().getDtFechamento() == null)
			throw new AplicacaoException(
					"N�o � poss�vel apensar a um documento n�o finalizado");

		if (!mobMestre.doc().isAssinado())
			throw new AplicacaoException(
					"N�o � poss�vel apensar a um documento n�o finalizado");

		if (mobMestre.isGeral())
			throw new Exception(
					"[E necess�rio definir a via ou volume do documento ao qual se quer apensar");

		if (!mobMestre.doc().isAssinado())
			throw new AplicacaoException(
					"N�o � poss�vel apensar a um documento n�o finalizado");

		if (mobMestre.isArquivado())
			throw new AplicacaoException(
					"N�o � poss�vel apensar a um documento arquivado");

		if (mobMestre.isJuntado())
			throw new AplicacaoException(
					"N�o � poss�vel apensar a um documento juntado");

		if (!getComp().podeMovimentar(cadastrante, lotaCadastrante, mobMestre))
			throw new AplicacaoException(
					"N�o � poss�vel apensar a um documento que esteja em outra lota��o");

		if (mobMestre.isEmTransito())
			throw new AplicacaoException(
					"N�o � poss�vel apensar a um documento em tr�nsito");

		if (mobMestre.isCancelada())
			throw new AplicacaoException(
					"N�o � poss�vel apensar a um documento cancelado");

		if (!mob.isEncerrado() && mobMestre.isEncerrado())
			throw new AplicacaoException(
					"N�o � poss�vel apensar um volume aberto a um volume encerrado");

		for (ExMobil apenso : mobMestre.getMobilETodosOsApensos(false)) {
			if (apenso.getIdMobil() == mob.getIdMobil()) {
				throw new AplicacaoException(
						"N�o � poss�vel apensar ao documento "
								+ mobMestre.getSigla()
								+ ", pois este j� est� apensado ao documento "
								+ mob.getSigla());
			}
		}

		if (!getComp().podeApensar(docTitular, lotaCadastrante, mob))
			throw new AplicacaoException(
					"A apensa��o do documento n�o ser realizada porque ele est� "
							+ "em transito, "
							+ "em transito externo, "
							+ "cancelado ou "
							+ "em local diferente da lota��o em que se encontra o documento ao qual se quer apensar");

		try {
			iniciarAlteracao();

			final ExMovimentacao mov = criarNovaMovimentacao(
					ExTipoMovimentacao.TIPO_MOVIMENTACAO_APENSACAO,
					cadastrante, lotaCadastrante, mob, dtMov, subscritor, null,
					titular, null, null);

			mov.setExMobilRef(mobMestre);

			mov.setDescrMov("Apensado ao documento "
					+ mov.getExMobilRef().getCodigo().toString());

			gravarMovimentacao(mov);
			concluirAlteracao(mov.getExDocumento());
		} catch (final Exception e) {
			cancelarAlteracao();
			throw new AplicacaoException("Erro ao apensar documento.", 0, e);
		}
	}

	/**
	 * Ap�s desapensar o mobil, copiamos do grande mestre o respons�vel e sua
	 * lota��o para o mobil em quest�o, de modo que nem todas as movimenta��es
	 * do grande mestre tenham que ser copiadas para todos os mobiles. Em
	 * especial, a transfer�ncia e o recebimento s� ser� copiado quando n�o se
	 * tratar de volume processual apensado ao pr�ximo.
	 * 
	 * @param cadastrante
	 * @param lotaCadastrante
	 * @param mob
	 * @param dtMov
	 * @param subscritor
	 * @param titular
	 * @throws AplicacaoException
	 */
	public void desapensarDocumento(final DpPessoa cadastrante,
			final DpLotacao lotaCadastrante, final ExMobil mob,
			final Date dtMov, final DpPessoa subscritor, final DpPessoa titular)
			throws AplicacaoException {

		if (!mob.isApensado())
			throw new AplicacaoException(
					"Imposs�vel desapensar documento que n�o est� apensado.");

		try {
			iniciarAlteracao();

			ExMobil mobMestre = mob.getGrandeMestre();

			final ExMovimentacao mov = criarNovaMovimentacao(
					ExTipoMovimentacao.TIPO_MOVIMENTACAO_DESAPENSACAO,
					cadastrante, lotaCadastrante, mob, dtMov, subscritor, null,
					titular, null, null);

			final ExMovimentacao ultMov = mob.getUltimaMovimentacao();

			ExMovimentacao movRef = mov.getExMobil().getUltimaMovimentacao(
					ExTipoMovimentacao.TIPO_MOVIMENTACAO_APENSACAO);
			mov.setExMovimentacaoRef(movRef);
			mov.setExMobilRef(movRef.getExMobilRef());
			mov.setDescrMov("Desapensado do documento "
					+ mov.getExMobilRef().getCodigo().toString());

			mov.setLotaResp(mobMestre.getUltimaMovimentacaoNaoCancelada()
					.getLotaResp());
			mov.setResp(mobMestre.getUltimaMovimentacaoNaoCancelada().getResp());

			gravarMovimentacao(mov);
			concluirAlteracao(mov.getExDocumento());
		} catch (final Exception e) {
			cancelarAlteracao();
			throw new AplicacaoException("Erro ao desapensar.", 0, e);
		}

	}

	public void encerrarAutomatico(final DpPessoa cadastrante,
			final DpLotacao lotaCadastrante, final ExMobil mob, final Date dtMov)
			throws AplicacaoException, Exception {

		if (mob.doc().isEletronico()) {
			dao().getSessao().refresh(mob);
			// Verifica se � Processo e conta o n�mero de p�ginas para verificar
			// se tem que fechar o volume
			if (mob.doc().isProcesso()) {
				if (mob.getTotalDePaginasSemAnexosDoMobilGeral() >= 200) {
					encerrar(cadastrante, lotaCadastrante, mob, dtMov, null,
							null, null, true);
				}
			}
		}
	}

	/**
	 * Encerra um volume e insere uma certid�o de encerramento de volume, que
	 * deve ser produzida, tanto em html quanto em pdf.
	 * 
	 * @param cadastrante
	 * @param lotaCadastrante
	 * @param mob
	 * @param dtMov
	 * @param subscritor
	 * @param titular
	 * @param nmFuncaoSubscritor
	 * @throws AplicacaoException
	 * @throws Exception
	 */
	public void encerrar(final DpPessoa cadastrante,
			final DpLotacao lotaCadastrante, final ExMobil mob,
			final Date dtMov, final DpPessoa subscritor,
			final DpPessoa titular, String nmFuncaoSubscritor,
			boolean automatico) throws AplicacaoException, Exception {

		if (mob.isEncerrado())
			throw new AplicacaoException(
					"N�o � permitido encerrar um documento que j� foi encerrado antes");

		if (!mob.isVolume())
			throw new AplicacaoException("S� � permitido encerrar volume");

		// if (responsavel == null && lotaResponsavel == null)
		// throw new AplicacaoException(
		// "N�o foram informados dados para o despacho/transfer�ncia");

		try {
			iniciarAlteracao();

			final ExMovimentacao mov = criarNovaMovimentacao(
					ExTipoMovimentacao.TIPO_MOVIMENTACAO_ENCERRAMENTO,
					cadastrante, lotaCadastrante, mob, dtMov, subscritor, null,
					titular, null, null);

			mov.setNmFuncaoSubscritor(nmFuncaoSubscritor);

			// Gravar o Html
			final String strHtml = processarModelo(mov, "processar_modelo",
					null, null);
			mov.setConteudoBlobHtmlString(strHtml);

			// Gravar o Pdf
			final byte pdf[] = Documento.generatePdf(strHtml);
			mov.setConteudoBlobPdf(pdf);
			mov.setConteudoTpMov("application/zip");

			if (automatico)
				mov.setDescrMov("Volume encerrado automaticamente.");

			gravarMovimentacao(mov);
			concluirAlteracao(mob.doc());
		} catch (final AplicacaoException e) {
			cancelarAlteracao();
			throw e;
		} catch (final Exception e) {
			cancelarAlteracao();
			throw new AplicacaoException("Erro ao encerrar.", 0, e);
		}
	}

	public String verificarAssinatura(byte[] conteudo, byte[] assinatura,
			String mimeType, Date dtAssinatura) throws Exception {
		String sNome;
		Long lCPF;

		// try {
		CdService client = Service.getCdService();

		sNome = client.validarAssinatura(assinatura, conteudo, dtAssinatura,
				VALIDAR_LCR);
		Service.throwExceptionIfError(sNome);

		String sCPF = client.recuperarCPF(assinatura);
		Service.throwExceptionIfError(sCPF);
		lCPF = Long.valueOf(sCPF);

		return sNome;

		// sNome = AssinaturaDigital.verificarAssinatura(movAlvo
		// .getConteudoBlobpdf(), assinatura, null);
		// } catch (final Exception e) {
		// throw new AplicacaoException("Erro na valida��o da assinatura. "
		// + e.getMessage(), 0, e);
		// }
	}

	public void gravarModelo(ExModelo modNovo, ExModelo modAntigo, Date dt,
			CpIdentidade identidadeCadastrante) throws AplicacaoException {
		if (modNovo.getExFormaDocumento() == null)
			throw new AplicacaoException(
					"N�o � poss�vel salvar um modelo sem informar a forma do documento.");
		if (modNovo.getNmMod() == null
				|| modNovo.getNmMod().trim().length() == 0)
			throw new AplicacaoException(
					"N�o � poss�vel salvar um modelo sem informar o nome.");
		if (modNovo.getDescMod() == null
				|| modNovo.getDescMod().trim().length() == 0)
			throw new AplicacaoException(
					"N�o � poss�vel salvar um modelo sem informar a descri��o.");
		try {
			ExDao.iniciarTransacao();
			dao().gravarComHistorico(modNovo, modAntigo, dt,
					identidadeCadastrante);
			ExDao.commitTransacao();
		} catch (Exception e) {
			ExDao.rollbackTransacao();
			throw new AplicacaoException("Erro ao salvar um modelo.", 0, e);
		}
	}

	public void DesfazerCancelamentoDocumento(DpPessoa cadastrante,
			final DpLotacao lotaCadastrante, ExDocumento doc) throws Exception {

		try {
			SortedSet<ExMobil> set = doc.getExMobilSet();

			iniciarAlteracao();

			for (ExMobil mob : set) {

				List<ExMovimentacao> movsCanceladas = mob
						.getMovimentacoesCanceladas();

				for (ExMovimentacao movARecuperar : movsCanceladas) {
					ExMovimentacao movCanceladora = movARecuperar
							.getExMovimentacaoCanceladora();

					movARecuperar.setExMovimentacaoCanceladora(null);

					gravarMovimentacao(movARecuperar);

					final ExMovimentacao novaMov = criarNovaMovimentacao(
							ExTipoMovimentacao.TIPO_MOVIMENTACAO_CANCELAMENTO_DE_MOVIMENTACAO,
							cadastrante, lotaCadastrante, mob, null, null,
							null, null, null, null);

					novaMov.setExMovimentacaoRef(movCanceladora);

					gravarMovimentacaoCancelamento(novaMov, movCanceladora);
				}
				concluirAlteracaoParcial(mob);
			}

			concluirAlteracao(null);

		} catch (final Exception e) {
			cancelarAlteracao();
			throw new AplicacaoException(
					"Erro ao anular cancelamento do documento.", 0, e);
		}
	}

	public void TornarDocumentoSemEfeito(DpPessoa cadastrante,
			final DpLotacao lotaCadastrante, ExDocumento doc, String motivo) throws Exception {
		try {
			iniciarAlteracao();

			final ExMovimentacao mov = criarNovaMovimentacao(
					ExTipoMovimentacao.TIPO_MOVIMENTACAO_TORNAR_SEM_EFEITO,
					cadastrante, lotaCadastrante, doc.getMobilGeral(), null,
					null, null, null, null, null);

			mov.setDescrMov(motivo);
			gravarMovimentacao(mov);

			concluirAlteracao(doc);
		} catch (final Exception e) {
			cancelarAlteracao();
			throw new AplicacaoException(
					"Erro ao tornar o documento sem efeito.", 0, e);
		}
	}

	public void alterarExClassificacao(ExClassificacao exClassNovo,
			ExClassificacao exClassAntigo, Date dt,
			CpIdentidade identidadeCadastrante) throws AplicacaoException {
		verificarDuplicacaoTermoCompleto(exClassNovo, exClassAntigo);
		try {

			dao().gravarComHistorico(exClassNovo, exClassAntigo, dt,
					identidadeCadastrante);
			copiarReferencias(exClassNovo, exClassAntigo, dt,
					identidadeCadastrante);

		} catch (Exception e) {
			throw new AplicacaoException(
					"Erro ao copiar as propriedades do modelo anterior.");
		}

	}

	private void copiarReferencias(ExClassificacao exClassNova,
			ExClassificacao exClassAntiga, Date dt,
			CpIdentidade identidadeCadastrante) throws AplicacaoException {
		copiarVias(exClassNova, exClassAntiga, dt, identidadeCadastrante);
		copiarModelos(exClassNova, exClassAntiga, dt, identidadeCadastrante);
	}

	private void copiarModelos(ExClassificacao exClassNovo,
			ExClassificacao exClassAntigo, Date dt,
			CpIdentidade identidadeCadastrante) throws AplicacaoException {
		try {
			// classificacao geral
			for (ExModelo modAntigo : exClassAntigo.getExModeloSet()) {
				ExModelo modNovo = new ExModelo();

				PropertyUtils.copyProperties(modNovo, modAntigo);
				modNovo.setIdMod(null);
				modNovo.setExClassificacao(exClassNovo);

				dao().gravarComHistorico(modNovo, modAntigo, dt,
						identidadeCadastrante);
				if (exClassNovo.getExModeloSet() == null) {
					exClassNovo.setExModeloSet(new HashSet<ExModelo>());
				}
				exClassNovo.getExModeloSet().add(modNovo);

			}

			// classificacao criacao via
			for (ExModelo modAntigo : exClassAntigo.getExModeloCriacaoViaSet()) {
				ExModelo modNovo = new ExModelo();

				PropertyUtils.copyProperties(modNovo, modAntigo);
				modNovo.setIdMod(null);
				modNovo.setExClassCriacaoVia(exClassNovo);

				dao().gravarComHistorico(modNovo, modAntigo, dt,
						identidadeCadastrante);
				if (exClassNovo.getExModeloCriacaoViaSet() == null) {
					exClassNovo
							.setExModeloCriacaoViaSet(new HashSet<ExModelo>());
				}
				exClassNovo.getExModeloCriacaoViaSet().add(modNovo);

			}

		} catch (Exception e) {
			throw new AplicacaoException(
					"N�o foi poss�vel fazer c�pia dos modelos!");
		}
	}

	private void copiarVias(ExClassificacao exClassNovo,
			ExClassificacao exClassAntigo, Date dt,
			CpIdentidade identidadeCadastrante) throws AplicacaoException {
		try {
			for (ExVia viaAntiga : exClassAntigo.getExViaSet()) {
				ExVia viaNova = new ExVia();

				PropertyUtils.copyProperties(viaNova, viaAntiga);
				viaNova.setId(null);
				viaNova.setExClassificacao(exClassNovo);

				dao().gravarComHistorico(viaNova, viaAntiga, dt,
						identidadeCadastrante);
				if (exClassNovo.getExViaSet() == null) {
					exClassNovo.setExViaSet(new HashSet<ExVia>());
				}
				exClassNovo.getExViaSet().add(viaNova);

			}
		} catch (Exception e) {
			throw new AplicacaoException(
					"N�o foi poss�vel fazer c�pia das vias!");
		}
	}

	public void moverClassificacao(ExClassificacao exClassDest,
			ExClassificacao exClassOrigem, CpIdentidade identidadeCadastrante)
			throws AplicacaoException {

		Date dt = dao().consultarDataEHoraDoServidor();
		MascaraUtil m = MascaraUtil.getInstance();
		ExDao dao = ExDao.getInstance();

		List<ExClassificacao> filhos = dao.consultarFilhos(exClassOrigem, true);

		if (filhos.size() > 0
				&& m.calcularNivel(exClassDest.getCodificacao()) > m
						.calcularNivel(exClassOrigem.getCodificacao())) {
			throw new AplicacaoException(
					"O n�vel do destino � maior do que o n�vel da origem! Os filhos n�o caber�o na hierarquia da classifica��o documental!");
		}

		ExClassificacao classExistente = dao().consultarPorSigla(exClassDest);
		if (classExistente != null) {
			throw new AplicacaoException(
					"A classifica��o destino j� existe! <br/><br/>"
							+ classExistente.getCodificacao());
		}

		String mascaraDestino = m.getMscFilho(exClassDest.getCodificacao(),
				true);
		for (ExClassificacao f : filhos) {
			String novaCodificacao = m.substituir(f.getCodificacao(),
					mascaraDestino);
			ExClassificacao fNovo = getCopia(f);
			fNovo.setCodificacao(novaCodificacao);
			Ex.getInstance()
					.getBL()
					.alterarExClassificacao(fNovo, f, dt, identidadeCadastrante);
		}

		exClassDest.setHisIdIni(exClassOrigem.getHisIdIni());
		Ex.getInstance()
				.getBL()
				.alterarExClassificacao(exClassDest, exClassOrigem, dt,
						identidadeCadastrante);
	}

	public ExClassificacao getCopia(ExClassificacao exClassOrigem)
			throws AplicacaoException {
		ExClassificacao exClassCopia = new ExClassificacao();
		try {

			PropertyUtils.copyProperties(exClassCopia, exClassOrigem);

			// novo id
			exClassCopia.setId(null);
			// objeto collection deve ser diferente (mas com mesmos elementos),
			// sen�o ocorre exception
			// HibernateException:Found shared references to a collection
			Set<ExVia> setExVia = new HashSet<ExVia>();
			exClassCopia.setExViaSet(setExVia);

			Set<ExModelo> setExModelo = new HashSet<ExModelo>();
			exClassCopia.setExModeloSet(setExModelo);

			Set<ExModelo> setExModeloCriacaoVia = new HashSet<ExModelo>();
			exClassCopia.setExModeloCriacaoViaSet(setExModeloCriacaoVia);

		} catch (Exception e) {
			throw new AplicacaoException(
					"Erro ao copiar as propriedades do modelo anterior.");
		}

		return exClassCopia;
	}

	public void incluirExClassificacao(ExClassificacao exClass,
			CpIdentidade identidadeCadastrante) throws AplicacaoException {
		verificarDuplicacaoTermoCompleto(exClass, null);
		dao().gravarComHistorico(exClass, null,
				dao().consultarDataEHoraDoServidor(), identidadeCadastrante);

	}

	private void verificarDuplicacaoTermoCompleto(ExClassificacao exClassNovo,
			ExClassificacao exClassAntigo) throws AplicacaoException {

		MascaraUtil m = MascaraUtil.getInstance();
		String mascara = m.getMscTodosDoNivel(m.calcularNivel(exClassNovo
				.getCodificacao()));
		List<ExClassificacao> exClassMesmoNivel = dao()
				.consultarExClassificacao(mascara,
						exClassNovo.getDescrClassificacao());
		if (exClassMesmoNivel.size() > 0) {
			for (ExClassificacao exClassConflito : exClassMesmoNivel) {

				if (exClassNovo.getDescricao().equalsIgnoreCase(
						exClassConflito.getDescricao())) {
					// se conflito n�o causado por movimentacao onde a pr�pria
					// classificacao � o conflito
					if (exClassAntigo == null
							|| !exClassConflito.getCodificacao().equals(
									exClassAntigo.getCodificacao())) {
						throw new AplicacaoException(
								"Termo da classifica��o em conflito! <br/><br/>"
										+ exClassConflito
												.getDescricaoCompleta());
					}

				}

			}
		}

	}

	public void incluirExTemporalidade(ExTemporalidade exTemporalidade,
			CpIdentidade identidadeCadastrante) throws AplicacaoException {
		ExDao.getInstance().gravarComHistorico(exTemporalidade, null, null,
				identidadeCadastrante);
	}

	public ExTemporalidade getCopia(ExTemporalidade exTempOrigem)
			throws AplicacaoException {
		ExTemporalidade exTempCopia = new ExTemporalidade();
		try {

			PropertyUtils.copyProperties(exTempCopia, exTempOrigem);

			// novo id
			exTempCopia.setId(null);
			// objeto collection deve ser diferente (mas com mesmos elementos),
			// sen�o ocorre exception
			// HibernateException:Found shared references to a collection
			Set<ExVia> setExViaArqCorr = new HashSet<ExVia>();
			Set<ExVia> setExViaArqInterm = new HashSet<ExVia>();
			exTempCopia.setExViaArqCorrenteSet(setExViaArqCorr);
			exTempCopia.setExViaArqIntermediarioSet(setExViaArqInterm);

		} catch (Exception e) {
			throw new AplicacaoException(
					"Erro ao copiar as propriedades do objeto ExTemporalidade.");
		}

		return exTempCopia;

	}

	public void alterarExTemporalidade(ExTemporalidade exTempNovo,
			ExTemporalidade exTempAntiga, Date dt,
			CpIdentidade identidadeCadastrante) throws AplicacaoException {

		dao().gravarComHistorico(exTempNovo, exTempAntiga, dt,
				identidadeCadastrante);

		// copiar Refer�ncias arq corrente
		try {
			for (ExVia viaAntiga : exTempAntiga.getExViaArqCorrenteSet()) {
				ExVia viaNova = new ExVia();

				PropertyUtils.copyProperties(viaNova, viaAntiga);
				viaNova.setIdVia(null);
				viaNova.setTemporalidadeCorrente(exTempNovo);

				dao().gravarComHistorico(viaNova, viaAntiga, dt,
						identidadeCadastrante);
				if (exTempNovo.getExViaArqCorrenteSet() == null) {
					exTempNovo.setExViaArqCorrenteSet(new HashSet<ExVia>());
				}
				exTempNovo.getExViaArqCorrenteSet().add(viaNova);

			}
		} catch (Exception e) {
			throw new AplicacaoException(
					"N�o foi poss�vel fazer c�pia das vias em arquivo corrente!");
		}

		// copiar Refer�ncias arq intermedi�rio
		try {
			for (ExVia viaAntiga : exTempAntiga.getExViaArqIntermediarioSet()) {
				ExVia viaNova = new ExVia();

				PropertyUtils.copyProperties(viaNova, viaAntiga);
				viaNova.setIdVia(null);
				viaNova.setTemporalidadeIntermediario(exTempNovo);

				dao().gravarComHistorico(viaNova, viaAntiga, dt,
						identidadeCadastrante);
				if (exTempNovo.getExViaArqIntermediarioSet() == null) {
					exTempNovo
							.setExViaArqIntermediarioSet(new HashSet<ExVia>());
				}
				exTempNovo.getExViaArqIntermediarioSet().add(viaNova);

			}
		} catch (Exception e) {
			throw new AplicacaoException(
					"N�o foi poss�vel fazer c�pia das vias em arquivo intermedi�rio!");
		}

	}

	public ExModelo getCopia(ExModelo exModOrigem) throws AplicacaoException {
		ExModelo exModCopia = new ExModelo();
		try {

			PropertyUtils.copyProperties(exModCopia, exModOrigem);

			// novo id
			exModCopia.setId(null);

		} catch (Exception e) {
			throw new AplicacaoException(
					"Erro ao copiar as propriedades do modelo anterior.");
		}

		return exModCopia;
	}

	public void excluirExClassificacao(ExClassificacao exClass,
			CpIdentidade idCadastrante) {
		Date dt = dao().consultarDataEHoraDoServidor();
		if (exClass.getExModeloSet().size() > 0
				|| exClass.getExModeloCriacaoViaSet().size() > 0) {
			StringBuffer sb = new StringBuffer();
			for (ExModelo m : exClass.getExModeloSet()) {
				sb.append("(");
				sb.append(m.getId());
				sb.append(") ");
				sb.append(m.getNmMod());
				sb.append("<br/>");
			}
			for (ExModelo m : exClass.getExModeloCriacaoViaSet()) {
				sb.append("(");
				sb.append(m.getId());
				sb.append(") ");
				sb.append(m.getNmMod());
				sb.append(" (Cria��o de via)");
				sb.append("<br/>");
			}

			throw new AplicacaoException(
					"N�o � poss�vel excluir a classifica��o documental, pois est� associada ao(s) seguinte(s) modelo(s):<br/><br/>"
							+ sb.toString());
		}

		/*AVISO: 
		 * 
		 *  
		 * O c�digo abaixo foi comentado para permitir a atualiza��o da tabela de classifica��o documental
		 * enquanto a funcionalidade de reclassifica��o de documentos n�a est� dispon�vel.
		 * 
		 * 
		 *  
		 */
		
		
		
		/*
		List<ExDocumento> docs = ExDao.getInstance()
				.consultarExDocumentoPorClassificacao(null,
						MascaraUtil.getInstance().getMscTodosDoMaiorNivel(),
						idCadastrante.getPessoaAtual().getOrgaoUsuario());
		if (docs.size() > 0) {
			StringBuffer sb = new StringBuffer();

			throw new AplicacaoException(
					"N�o � poss�vel excluir a classifica��o documental, pois j� foi associada a documento(s)."
							+ sb.toString());
		}
		 */
		
		
		
		
		for (ExVia exVia : exClass.getExViaSet()) {
			dao().excluirComHistorico(exVia, dt, idCadastrante);
		}
		dao().excluirComHistorico(exClass, dt, idCadastrante);
	}

}
