<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<%@ page language="java" contentType="text/html; charset=UTF-8"
	buffer="64kb"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib prefix="ww" uri="/webwork"%>
<ww:select
	label="Tipo" name="idFormaDoc" id="forma"
	headerKey="0" headerValue="[Todos]"
	list="todasFormasDocPorTipoForma" listKey="idFormaDoc"
	listValue="descrFormaDoc" theme="simple" onchange="javascript:alteraForma();" />