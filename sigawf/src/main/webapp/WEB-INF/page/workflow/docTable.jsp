<%@ page language="java" contentType="text/html; charset=ISO-8859-1"%> <%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%><%@ taglib prefix="ww" uri="/webwork"%> <%@ taglib uri="http://localhost/customtag" prefix="tags"%><!--  -->
<input type="hidden" name="pdId" value="${pd.id}" />
<input type="hidden" name="piId" value="${pi.id}" />
<input type="hidden" name="tiId" value="${ti.id}" />
<input type="hidden" name="trId" />
<table class="list" width="100%" border="0"  style="margin:0px;padding:0px;border:0px;">
	<c:import url="docTitle.jsp" />
	<c:import url="variables.jsp" />
	<c:import url="transactions.jsp" />
</table>