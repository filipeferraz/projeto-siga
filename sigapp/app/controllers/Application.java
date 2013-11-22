package controllers;

import groovy.sql.Sql;
import groovy.ui.text.FindReplaceUtility;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.sql.*;

import javax.management.Query;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.PersistenceException;
import javax.swing.plaf.metal.MetalBorders.Flush3DBorder;

import net.sf.ehcache.hibernate.management.impl.NullHibernateStats;

import org.apache.commons.collections.functors.NullIsExceptionPredicate;
import org.apache.commons.lang.NullArgumentException;
import org.apache.commons.lang.time.DateFormatUtils;
import org.codehaus.groovy.runtime.DateGroovyMethods;
import org.codehaus.groovy.runtime.NullObject;
import org.hibernate.PersistentObjectException;
import org.hibernate.bytecode.buildtime.ExecutionException;
import org.hibernate.cfg.annotations.Nullability;
import org.hibernate.ejb.EntityManagerFactoryImpl;
import org.hibernate.ejb.EntityManagerImpl;
import org.hibernate.engine.transaction.NullSynchronizationException;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.exception.SQLExceptionConverter;
import org.hibernate.util.JDBCExceptionReporter.WarningHandler;

import com.sun.org.apache.bcel.internal.ExceptionConstants;

import br.gov.jfrj.siga.base.DateUtils;
import br.gov.jfrj.siga.base.util.MatriculaUtils;
import br.gov.jfrj.siga.dp.DpPessoa;

import oracle.jdbc.driver.SQLUtil;

import models.Agendamentos;
import models.Foruns;
import models.Locais;
import models.UsuarioForum;
import play.db.helper.JpqlSelect;
import play.db.helper.SqlQuery;
import play.db.helper.SqlUnion;
import play.db.jpa.JPA;
import play.exceptions.PlayException;
import play.mvc.Before;
import play.mvc.Catch;
import play.mvc.Scope;
import sun.misc.JavaLangAccess;

public class Application extends SigaApplication {

	@Before
	public static void addDefaultsAlways() throws Exception {
		prepararSessao();
	}

	@Before
	public static void addDefaults() throws Exception {

		try {
			obterCabecalhoEUsuario("rgb(235, 235, 232)");
		} catch (Exception e) {
			tratarExcecoes(e);
		}
	}

	protected static void assertAcesso(String path) throws Exception {
		// SigaApplication.assertAcesso("SR:Módulo de Serviços;" + path);
	}

	@Catch()
	public static void tratarExcecoes(Exception e) {
		SigaApplication.tratarExcecoes(e);
	}

	public static void index() {
		// desativado
		render();
		// models.Locais role
	}

	public static void home() {
		// página inicial do sigapmp
		String matriculaSessao = cadastrante().getMatricula().toString();
		UsuarioForum objUsuario = UsuarioForum.find("matricula_usu =" + matriculaSessao).first();
		if(objUsuario!=null){
			try{
				List<Locais> lstLocais =  Locais.find("forumFk="+objUsuario.forumFk.cod_forum+"order by ordem_apresentacao ").fetch();
				render(lstLocais);
			}catch(Exception e){
				e.printStackTrace();
			}
		}else{
			Excecoes("Usuário sem permissão");
		}
	}

	public static void sala_incluir() {
		// pega usuario do sistema
		String matriculaSessao = cadastrante().getMatricula().toString();
		UsuarioForum objUsuario = UsuarioForum.find(
				"matricula_usu =" + matriculaSessao).first();
		if (objUsuario != null) {
			render();
		} else {
			Excecoes("Usuário sem permissão");
		}
	}

	public static void sala_insert(Locais formLocal, int cod_forum) {
		Foruns objForum = new Foruns(cod_forum, " ");
		formLocal.forumFk = objForum;
		String varCodLocal = formLocal.cod_local;
		String resposta = "";
		try {
			System.out.println(JPA
					.em()
					.createQuery(
							"from Locais where cod_local ='" + varCodLocal
									+ "'").getSingleResult());
			resposta = "Sala já existe. Confira o código da sala. ";
		} catch (Exception e) {
			try {
				System.out.println(JPA.em().createQuery(
						"from Foruns where cod_forum=" + cod_forum));
				formLocal.save();
				JPA.em().flush();
				resposta = "Ok.";
			} catch (Exception e2) {
				resposta = "Forum da sala não cadastrado. Confira o código do forum.";
			}
		}
		render(resposta);
	}

	public static void salas_listar(String cod_local, String sala,
			String desc_forum) {
		// pega usuario do sistema
		String matriculaSessao = cadastrante().getMatricula().toString();
		UsuarioForum objUsuario = UsuarioForum.find(
				"matricula_usu =" + matriculaSessao).first();
		if (objUsuario != null) {
			List<Locais> listLocais = new ArrayList<Locais>();
			if (desc_forum == null) {
				desc_forum = "";
			}
			if (cod_local == null) {
				cod_local = "";
			}
			if (sala == null) {
				sala = "";
			}
			if (desc_forum.isEmpty() && cod_local.isEmpty() && sala.isEmpty()) {
				listLocais = Locais.findAll();
				render(listLocais);
			} else if (!cod_local.isEmpty()) {
				try {
					Object auxLocal = Locais.findById(cod_local);
					if (auxLocal.equals(null)) {
						// retornou nulo
					} else {
						listLocais.add((Locais) auxLocal);
					}
				} finally {
					render(listLocais);
				}
			} else if (!sala.isEmpty()) {
				try {
					listLocais = JPA
							.em()
							.createQuery(
									"from Locais where local like '" + sala
											+ "%'").getResultList();
				} catch (ExecutionException e) {
					e.printStackTrace();
				} finally {
					render(listLocais);
				}
			} else if (!desc_forum.isEmpty()) {
				try {
					List<Foruns> listForuns = new ArrayList<Foruns>();
					Foruns objForum = new Foruns(0, "");
					List<Locais> listLocaisAux = new ArrayList<Locais>();
					listForuns = JPA
							.em()
							.createQuery(
									"from Foruns where descricao_forum like'"
											+ desc_forum + "%'")
							.getResultList();
					for (Iterator iterator01 = listForuns.iterator(); iterator01
							.hasNext();) {
						objForum = (Foruns) iterator01.next();
						listLocaisAux = JPA
								.em()
								.createQuery(
										"from Locais where forumFk ="
												+ objForum.cod_forum)
								.getResultList();
						for (Iterator iterator02 = listLocaisAux.iterator(); iterator02
								.hasNext();) {
							listLocais.add((Locais) iterator02.next());
							System.out.println("- Detalhe -");
						}
					}
				} catch (ExecutionException e) {
					e.printStackTrace();
				} finally {
					render(listLocais);
				}
			}
		} else {
			Excecoes("Usuário sem permissão");
		}
	}

	public static void sala_delete(String cod_sala) {
		String resposta = " ";
		if (!cod_sala.isEmpty()) {
			try{
			resposta = "Ok.";
			Locais.delete("from Locais where cod_local='" + cod_sala + "'");
			}catch(Exception e){
				e.printStackTrace();
				resposta="Esta sala possui agendamentos. Delete primeiro os agendamentos referenciados.";
			}
		} else {
			resposta = "Não Ok.";
		}
		render(resposta);
	}

	public static void forum_incluir() {
		// pega usuario do sistema
		String matriculaSessao = cadastrante().getMatricula().toString();
		UsuarioForum objUsuario = UsuarioForum.find(
				"matricula_usu =" + matriculaSessao).first();
		if (objUsuario != null) {
			render();
		} else {
			Excecoes("Usuário sem permissão");
		}
	}

	public static void forum_insert(int cod_forum, String descricao) {
		String resposta = "";
		try {
			Foruns objForum = new Foruns(cod_forum, descricao);
			objForum.save();
			JPA.em().flush();
			resposta = "Ok.";
		} catch (PersistenceException e) {
			e.printStackTrace();
			resposta = "Não Ok.";
			if (e.getMessage().substring(23, 53)
					.equals(".ConstraintViolationException:")) {
				resposta = "Fórum já existe. Verifique se o código está correto.";
			}
		} finally {
			render(resposta);
		}
	}

	public static void foruns_listar(Integer cod_forum, String descricao_forum) {
		// pega usuario do sistema
		String matriculaSessao = cadastrante().getMatricula().toString();
		UsuarioForum objUsuario = UsuarioForum.find(
				"matricula_usu =" + matriculaSessao).first();
		if (objUsuario != null) {
			List<Foruns> listForuns = new ArrayList<Foruns>();
			if (cod_forum == null) {
				cod_forum = -1;
			}
			if (descricao_forum == null) {
				descricao_forum = "";
			}
			if (cod_forum == -1 && descricao_forum.isEmpty()) {
				listForuns = Foruns.findAll();
				render(listForuns);
			} else if (cod_forum >= 0) {
				Object auxForum = Foruns.findById(cod_forum);
				if (auxForum != null) {
					listForuns.add((Foruns) auxForum);
				}
				render(listForuns);
			} else if (!descricao_forum.isEmpty()) {
				List<Foruns> resultado = JPA
						.em()
						.createQuery(
								"from Foruns where descricao_forum like '"
										+ descricao_forum + "%'")
						.getResultList();
				for (Iterator iterator = resultado.iterator(); iterator
						.hasNext();) {
					listForuns.add((Foruns) iterator.next());
				}
				render(listForuns);
			}
		} else {
			Excecoes("Usuário sem permissão");
		}
	}

	public static void forum_delete(String cod_forum) {
		String resposta;
		if (!cod_forum.isEmpty()) {
			resposta = "Ok.";
			try {
				Foruns.delete("from Foruns where cod_forum=" + cod_forum);
			} catch (PersistenceException e) {
				if (e.getMessage().substring(23, 53)
						.equals(".ConstraintViolationException:")) {
					System.out.println(e.getMessage());
					resposta = "Forum possui referência. Delete primeiro as salas referenciadas.";
				} else {
					resposta = "erro.";
				}

			} finally {
				render(resposta);
			}
		} else {
			resposta = "Não Ok.";
			render(resposta);
		}
	}
	
	public static void agendamento_incluir_ajax(){
		List<Locais> listSalas = new ArrayList();
		// pega usuario do sistema
		String matriculaSessao = cadastrante().getMatricula().toString();
		UsuarioForum objUsuario = UsuarioForum.find(
				"matricula_usu =" + matriculaSessao).first();
		if (objUsuario != null) {
			// Pega o usuário do sistema, e, busca os locais(salas) daquele
			// forum onde ele está.
			listSalas = (List) Locais.find("cod_forum='"+objUsuario.forumFk.cod_forum+"' order by ordem_apresentacao ").fetch(); // isso não dá erro no caso de retorno vazio.
			render(listSalas);
		}else{
			Excecoes("Usuário sem permissão");
		}
	}
	
	public static void calendario_vetor(String frm_cod_local){
		List listDatasLotadas = new ArrayList();
		List listDatasDoMes = new ArrayList();
		SimpleDateFormat df = new SimpleDateFormat("dd/MM/yyyy");
		Date parametro = new Date();
		Date dt = new Date();
		String dtt = df.format(dt);
		Agendamentos objAgendamento = new Agendamentos(parametro,
				null, null, null, null, null, null, null, null);
		try {
			List<Agendamentos> results = Agendamentos
					.find("data_ag >= to_date('" + dtt.trim()
							+ "','dd/MM/yyyy') and cod_local='"
							+ frm_cod_local.trim() + "'  order by data_ag")
					.fetch();
			// verifica se veio algum agendamento
			if (results.size() != 0) {
				// preenche as datas do local no 'MÊS' na agenda
				// CORRENTE
				for (Iterator it = results.iterator(); it.hasNext();) {
					objAgendamento = (Agendamentos) it.next();
					listDatasDoMes.add(objAgendamento.data_ag
							.toString());
				}
				String dia_ag_ant = "";
				String dia_ag_prox;
				int i = 0;
				// conta os agendamentos de cada dia, do local que
				// veio do form
				for (Iterator it = listDatasDoMes.iterator(); it
						.hasNext();) {
					dia_ag_prox = (String) it.next(); // pegou o próximo
					if (i == 0) {
						dia_ag_ant = dia_ag_prox;
					}
					if (dia_ag_prox.equals(dia_ag_ant)) {
						i++; // contou a repetição
					} else {
						i = 1;
						dia_ag_ant = dia_ag_prox;
					}
					// se a data estiver lotada, marca
					if (i >= 49) {
						listDatasLotadas.add(dia_ag_ant);
					} // guardou a data lotada
				}
				// veio algum agendamento
				System.out.println(results.size()+" agendamentos...");
			} else {
				// não veio nenhum agendamento
			}
		} catch (Exception e){
			e.printStackTrace();
		}
		render(listDatasLotadas);
	}
	
	public static void horario_vetor(String frm_cod_local, String frm_data_ag){
		List<String> listHorasLivres = new ArrayList<String>();
		SimpleDateFormat df = new SimpleDateFormat("dd/MM/yyyy");
		Date dt = new Date();
		String dtt = df.format(dt);
		Agendamentos objAgendamento = new Agendamentos(null,
				null, null, null, null, null, null, null, null);
		List<Agendamentos> results = new ArrayList<Agendamentos>();
		if (frm_data_ag != null && !frm_data_ag.isEmpty()) {
			listHorasLivres.add("09:00");
			listHorasLivres.add("09:10");
			listHorasLivres.add("09:20");
			listHorasLivres.add("09:30");
			listHorasLivres.add("09:40");
			listHorasLivres.add("09:50");
			listHorasLivres.add("10:00");
			listHorasLivres.add("10:10");
			listHorasLivres.add("10:20");
			listHorasLivres.add("10:30");
			listHorasLivres.add("10:40");
			listHorasLivres.add("10:50");
			listHorasLivres.add("11:00");
			listHorasLivres.add("11:10");
			listHorasLivres.add("11:20");
			listHorasLivres.add("11:30");
			listHorasLivres.add("11:40");
			listHorasLivres.add("11:50");
			listHorasLivres.add("12:00");
			listHorasLivres.add("12:10");
			listHorasLivres.add("12:20");
			listHorasLivres.add("12:30");
			listHorasLivres.add("12:40");
			listHorasLivres.add("12:50");
			listHorasLivres.add("13:00");
			listHorasLivres.add("13:10");
			listHorasLivres.add("13:20");
			listHorasLivres.add("13:30");
			listHorasLivres.add("13:40");
			listHorasLivres.add("13:50");
			listHorasLivres.add("14:00");
			listHorasLivres.add("14:10");
			listHorasLivres.add("14:20");
			listHorasLivres.add("14:30");
			listHorasLivres.add("14:40");
			listHorasLivres.add("14:50");
			listHorasLivres.add("15:00");
			listHorasLivres.add("15:10");
			listHorasLivres.add("15:20");
			listHorasLivres.add("15:30");
			listHorasLivres.add("15:40");
			listHorasLivres.add("15:50");
			listHorasLivres.add("16:00");
			listHorasLivres.add("16:10");
			listHorasLivres.add("16:20");
			listHorasLivres.add("16:30");
			listHorasLivres.add("16:40");
			listHorasLivres.add("16:50");
			listHorasLivres.add("17:00");
			df.applyPattern("dd-MM-yyyy");
			try {
				dtt = frm_data_ag;
				results = Agendamentos
						.find("data_ag = to_date('"
								+ dtt.trim()
								+ "','dd-mm-yy') and cod_local='"
								+ frm_cod_local.trim() + "'")
						.fetch();
				// zera os horários ocupados na frm_data_ag
				// selecionada, no local frm_cod_local
				String hrr = "";
				for (Iterator it = results.iterator(); it
						.hasNext();) {
					objAgendamento = (Agendamentos) it
							.next();
					hrr = objAgendamento.hora_ag;
					hrr = hrr.substring(0, 2) + ":"
							+ hrr.substring(2, 4);
					listHorasLivres.set(
							listHorasLivres.indexOf(hrr),
							"");
				}
			} catch (Exception e) {
				e.printStackTrace();
			}finally{
				render(listHorasLivres);
			}
		}

	}
		
	// o código abaixo NÃO será usado, mas, deverá ser preservado porque está funcionando bem.
	public static void agendamento_incluir_puro(String frm_cod_local,
			String frm_data_ag, Boolean verde) {
		// pega usuario do sistema
		String matriculaSessao = cadastrante().getMatricula().toString();
		UsuarioForum objUsuario = UsuarioForum.find(
				"matricula_usu =" + matriculaSessao).first();
		if (objUsuario != null) {
			List<String> listHorasLivres = new ArrayList<String>();
			List<Locais> listSalas = new ArrayList();
			List listDatasLotadas = new ArrayList();
			List listDatasDoMes = new ArrayList();
			if (frm_data_ag == null && frm_cod_local == null) { // Se nulo
				// Pega o usuário do sistema, e, busca os locais(salas) daquele
				// forum onde ele está.
				listSalas = (List) Locais.find("cod_forum='"+objUsuario.forumFk.cod_forum+"'").fetch(); // isso não dá erro no caso de retorno vazio.
			} else {
				if (frm_cod_local != null) {
					SimpleDateFormat df = new SimpleDateFormat("dd/MM/yyyy");
					Date parametro = new Date();
					Date dt = new Date();
					String dtt = df.format(dt);
					Agendamentos objAgendamento = new Agendamentos(parametro,
							null, null, null, null, null, null, null, null);
					try {
						List<Agendamentos> results = Agendamentos
								.find("data_ag >= to_date('" + dtt
										+ "','dd/MM/yyyy') and cod_local='"
										+ frm_cod_local + "'  order by data_ag")
								.fetch();
						// verifica se veio algum agendamento
						if (results.size() != 0) {
							// preenche as datas do local no 'MÊS' na agenda
							// CORRENTE
							for (Iterator it = results.iterator(); it.hasNext();) {
								objAgendamento = (Agendamentos) it.next();
								listDatasDoMes.add(objAgendamento.data_ag
										.toString());
							}
							String dia_ag_ant = "";
							String dia_ag_prox;
							int i = 0;
							// conta os agendamentos de cada dia, do local que
							// veio do form
							for (Iterator it = listDatasDoMes.iterator(); it
									.hasNext();) {
								dia_ag_prox = (String) it.next(); // pegou o próximo
								if (i == 0) {
									dia_ag_ant = dia_ag_prox;
								}
								if (dia_ag_prox.equals(dia_ag_ant)) {
									i++; // contou a repetição
								} else {
									i = 1;
									dia_ag_ant = dia_ag_prox;
								}
								// se a data estiver lotada, marca
								if (i >= 33) {
									listDatasLotadas.add(dia_ag_ant);
								} // guardou a data lotada
							}
							// veio algum agendamento
							System.out.println(results.size()
									+ " agendamentos...");
						} else {
							// não veio nenhum agendamento
							System.out.println("não veio agendamento nenhum");
							System.out.println("Data ag:" + frm_data_ag);
						}
						// frm_cod_local não veio nulo
						listSalas.clear();
						listSalas.add((Locais) Locais.find(
								"cod_local = '" + frm_cod_local + "'").first());
						// escolheu uma sala, e, submeteu o form a si
						if (verde) {
							// local com dias filtrados, passa aos horários do
							// dia selecionado
							if (frm_data_ag != null && !frm_data_ag.isEmpty()) {
								listHorasLivres.add("10:00");
								listHorasLivres.add("10:30");
								listHorasLivres.add("11:00");
								listHorasLivres.add("11:30");
								listHorasLivres.add("12:00");
								listHorasLivres.add("12:30");
								listHorasLivres.add("13:00");
								listHorasLivres.add("13:30");
								listHorasLivres.add("14:00");
								listHorasLivres.add("14:30");
								listHorasLivres.add("15:00");
								listHorasLivres.add("15:30");
								listHorasLivres.add("16:00");
								listHorasLivres.add("16:30");
								listHorasLivres.add("17:00");
								listHorasLivres.add("17:30");
								listHorasLivres.add("18:00");
								listHorasLivres.add("18:30");
								df.applyPattern("dd-MM-yyyy");
								try {
									parametro = df.parse(frm_data_ag);
								} catch (Exception e) {
									e.printStackTrace();
								}
								objAgendamento = null;
								/*
								Locais objLocal = Locais.find(
										"cod_local = '" + frm_cod_local + "'")
										.first();
								listSalas.add(objLocal);
								*/
								try {
									dtt = frm_data_ag;
									results.clear();
									results = Agendamentos
											.find("data_ag = to_date('"
													+ dtt
													+ "','dd-MM-yyyy') and cod_local='"
													+ frm_cod_local + "'")
											.fetch();
									// zera os horários usados na data
									// selecionada no determinado local
									String hrr = "";
									for (Iterator it = results.iterator(); it
											.hasNext();) {
										objAgendamento = (Agendamentos) it
												.next();
										hrr = objAgendamento.hora_ag;
										hrr = hrr.substring(0, 2) + ":"
												+ hrr.substring(2, 4);
										listHorasLivres.set(
												listHorasLivres.indexOf(hrr),
												"");
									}
								} catch (Exception e) {
									e.printStackTrace();
								}
							}
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
			try {
				render(listSalas, listHorasLivres, listDatasLotadas,
						frm_data_ag);
			} catch (Exception e) {
				e.printStackTrace();

			}
		} else {
			Excecoes("Usuário sem permissão");
		}
	}

	public static void agendamento_insert(String frm_data_ag, String frm_hora_ag,
			String frm_cod_local, String matricula, String periciado,
			String perito_juizo, String perito_parte, String orgao,
			String processo) {
		System.out.println(" === ");
		System.out.println(frm_hora_ag);
		System.out.println(" === ");
		matricula = cadastrante().getMatricula().toString();
		String resposta = "";
		Locais auxLocal = new Locais();
		String hr;
		auxLocal.cod_local = frm_cod_local;
		auxLocal.local = "";
		auxLocal.dias = "";
		auxLocal.endereco = "";
		auxLocal.exibir = 1;
		auxLocal.forumFk = null;
		auxLocal.hora_fim = "";
		auxLocal.hora_ini = "";
		auxLocal.intervalo_atendimento = 0;
		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
		try {
			Date parametro = df.parse(frm_data_ag);
			Agendamentos objAgendamento = new Agendamentos(parametro, frm_hora_ag,
					auxLocal, matricula , periciado, perito_juizo, perito_parte,
					processo, orgao);
			hr = frm_hora_ag;
			if (hr != null && (!hr.isEmpty())) {
				objAgendamento.hora_ag = hr.substring(0, 2)
						+ hr.substring(3, 5);
			
			objAgendamento.save();
			JPA.em().flush();
			resposta = "Ok.";
			}else{resposta="Não Ok.";}
		} catch (Exception e) {
			e.printStackTrace();
			resposta = "Não Ok. Verifique se preencheu todos os campos do agendamento.";
		} finally {
			render(resposta);
		}
	}

	public static void agendamento_excluir(String frm_data_ag) {
		// pega matricula do usuario do sistema
		String matriculaSessao = cadastrante().getMatricula().toString();
		// pega a permissão do usuario
		UsuarioForum objUsuario = UsuarioForum.find(
				"matricula_usu =" + matriculaSessao).first();
		// verifica se tem permissão
		if (objUsuario != null) {
			List<Agendamentos> listAgendamentos = new ArrayList<Agendamentos>();
			// verifica se o formulário submeteu alguma data
			if (frm_data_ag != null) {
				// Busca os agendamentos da data do formulário
				listAgendamentos = Agendamentos.find(
						"data_ag = to_date('" + frm_data_ag
								+ "','dd-mm-yy') order by data_ag , hora_ag").fetch();
				// busca os locais do forum do usuario
				List<Locais> listLocais = Locais.find("cod_forum='"+objUsuario.forumFk.cod_forum+"'").fetch();
				// Verifica se existe local naquele forum do usuário
				if(listAgendamentos.size()!=0){
					// para cada agendamento, inlcui na lista a sala que é do forum daquele usuário
					List<Agendamentos> auxAgendamentos = new ArrayList<Agendamentos>();
					for(Integer i=0; i<listAgendamentos.size();i++ ){
						// pega o agendamento 
						for(Integer ii=0; ii<listLocais.size();ii++){
							// varre os locais do forum
							if(listAgendamentos.get(i).localFk.cod_local==listLocais.get(ii).cod_local){
								// pertence à lista de agendamentos do forum do usuario
								auxAgendamentos.add((Agendamentos)listAgendamentos.get(i));
							}
						}
					}
					listLocais.clear();
					listAgendamentos.clear();
					listAgendamentos.addAll(auxAgendamentos);
					auxAgendamentos.clear();
				}
			}
			if (listAgendamentos.size() != 0) {
				render(listAgendamentos);
			} else {
				render();
			}
		} else {
			Excecoes("Usuário sem permissão");
		}

	}

	public static void agendamento_delete(Agendamentos formAgendamento,
			String cod_local) {
		String resultado = "";
		SimpleDateFormat df = new SimpleDateFormat("dd/MM/yyyy");
		String dtt = df.format(formAgendamento.data_ag);
		try {
			Agendamentos ag = Agendamentos.find(
					"hora_ag = '" + formAgendamento.hora_ag
							+ "' and localFk.cod_local='" + cod_local
							+ "' and data_ag = to_date('" + dtt
							+ "','dd/mm/yy')").first();
			ag.delete();
			resultado = "Ok.";
		} catch (Exception e) {
			e.printStackTrace();
			resultado = "Não Ok.";
		} finally {
			render(resultado);
		}
	}

	public static void agendadas_hoje() {
		// pega usuário do sistema
		String matriculaSessao = cadastrante().getMatricula().toString();
		UsuarioForum objUsuario = UsuarioForum.find(
				"matricula_usu =" + matriculaSessao).first();
		if (objUsuario != null) {
			SimpleDateFormat df = new SimpleDateFormat("dd-MM-yyyy");
			Date hoje = new Date();
			String dtt = df.format(hoje);
			// busca agendamentos de hoje
			List<Agendamentos> listAgendamentos = Agendamentos.find(
					"data_ag = to_date('" + dtt
							+ "','dd-mm-yy') order by hora_ag").fetch();
			if (listAgendamentos.size() != 0) {
				// busca as salas daquele forum
				List<Locais> listLocais = Locais.find("cod_forum='"+ objUsuario.forumFk.cod_forum + "'").fetch();
				// lista auxiliar
				List<Agendamentos> auxAgendamentos = new ArrayList<Agendamentos>();
				for(int i=0;i<listAgendamentos.size();i++){
					//varre listAgendamentos
					for(int ii=0;ii<listLocais.size();ii++){
						// compara com cada local do forum do usuário
						if(listAgendamentos.get(i).localFk.cod_local==listLocais.get(ii).cod_local){
							auxAgendamentos.add((Agendamentos)listAgendamentos.get(i));
						}
					}
				}
				render(listAgendamentos);
			} else {
				render();
			}
		} else {
			Excecoes("Usuário sem permissão");
		}

	}

	public static void usuario_atualiza(String paramCodForum) throws Exception{
		String mensagem="";
    	// pega usuario do sistema
    	String matriculaSessao = cadastrante().getMatricula().toString();
    	String nomeSessao = cadastrante().getNomeAbreviado();
    	UsuarioForum objUsuario = UsuarioForum.find("matricula_usu ="+ matriculaSessao).first();
    	if(objUsuario!=null){
    		if(paramCodForum!=null && !paramCodForum.isEmpty()){
    			Foruns objForum = new Foruns(Integer.parseInt(paramCodForum), "");
    			objUsuario.delete();
    			objUsuario.forumFk = objForum;
    			objUsuario.matricula_usu = matriculaSessao;
    			objUsuario.nome_usu =  nomeSessao;
    			try{
    				objUsuario.save();
    				JPA.em().flush();
    				mensagem = "Ok.";
    			}catch (Exception e){
    				e.printStackTrace();
    				mensagem = "Não Ok.";
    			}
    			System.out.println("saved"+objUsuario.forumFk.cod_forum);
    		}else{
    			paramCodForum = Integer.toString(objUsuario.forumFk.cod_forum);
    		}
    		System.out.println(objUsuario.matricula_usu);
    		render(objUsuario, paramCodForum, mensagem);
    	}else{
    		Excecoes("Usuário sem permissão");
    	}
    }

	public static void creditos() {
		render();
	}

	@Catch
	public static void Excecoes(String e) {
		String msg = e;
		System.out.println("+++ Excecoes  Exceptions  +++");
		render("Application/erro.html", msg);
	}
}