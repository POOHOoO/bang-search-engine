<html>
<head>
<title>Bang!!</title>
<style type="text/css">
body {
 background: url('../res/fireworks.jpg') no-repeat;
 background-size: 100%;
 margin: 0 auto 0 auto;
 padding: 0;
 text-align: center;
 font-family: Arial, sans-serif;
}
div#logo {
 margin: 50px auto 0 auto;
 background: url('../res/bang.png') #ffffff;;
 border: 3px double #333333;
 width: 420px;
 height: 160px;
}
div#search {
 margin: 30px auto 0 auto;
 background: #ffffff;
 border: 3px double #333333;
 width: 600px;
 height: 200px;
}
div#search p {
 font-size: 12pt;
 margin: 10px 0 0 0;
 padding: 0;
}
div#search input#q {
 margin: 35px 0 10px 0;
 font-size: 20pt;
}
div#search input.button {
 font-size: 16pt;
 font-weight: bold;
}
</style>
</head>
<body onload="document.bang.q.focus();">

<div id="logo"></div>

<div id="search">
<form name="search" method="GET" action="bang/###BANG_SEARCH_SUBMIT_PATH###"/>
<input id="q" type="text" size="35" name="###BANG_QUERY_PARAM###" value=""/><br/>
<input class="button" type="submit" name="submit" value="Bang It!"/>&nbsp;
<input class="button" type="submit" name="###BANG_FEELING_LUCKY_PARAM###" value="Feeling Lucky?"/><br/>
<p><input type="checkbox" name="###BANG_SEARCH_MODE_INCLUDE_YAHOO###" value="###BANG_PARAM_TRUE_VALUE###" checked="checked"/>&nbsp;Include Yahoo! results</p>
<p><input type="checkbox" name="###BANG_SEARCH_MODE_INCLUDE_AMAZON###" value="###BANG_PARAM_TRUE_VALUE###" checked="checked"/>&nbsp;Include Amazon results</p>
</form>
</div>

</body>
</html>
