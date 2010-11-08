/*
 * Copyright (C) 2009 Emweb bvba, Leuven, Belgium.
 *
 * See the LICENSE file for terms of use.
 */
package eu.webtoolkit.jwt;


class SizeHandle {
	public static void loadJavaScript(WApplication app) {
		String THIS_JS = "js/SizeHandle.js";
		if (!app.isJavaScriptLoaded(THIS_JS)) {
			app.doJavaScript(wtjs1(app), false);
			app.setJavaScriptLoaded(THIS_JS);
		}
	}

	static String wtjs1(WApplication app) {
		return "Wt3_1_7.SizeHandle = function(c,j,e,m,p,q,r,s,g,h,i,k,l){function n(b){b=b.changedTouches?{x:b.changedTouches[0].pageX,y:b.changedTouches[0].pageY}:c.pageCoordinates(b);return Math.min(Math.max(j==\"h\"?b.x-f.x-d.x:b.y-f.y-d.y,p),q)}var a=document.createElement(\"div\");a.style.position=\"absolute\";a.style.zIndex=\"100\";if(j==\"v\"){a.style.width=m+\"px\";a.style.height=e+\"px\"}else{a.style.height=m+\"px\";a.style.width=e+\"px\"}var f,d=c.widgetPageCoordinates(g);e=c.widgetPageCoordinates(h);if(i.touches)f= c.widgetCoordinates(g,i.touches[0]);else{f=c.widgetCoordinates(g,i);c.capture(null);c.capture(a)}k-=c.px(g,\"marginLeft\");l-=c.px(g,\"marginTop\");d.x+=k-e.x;d.y+=l-e.y;f.x-=k-e.x;f.y-=l-e.y;a.style.left=d.x+\"px\";a.style.top=d.y+\"px\";a.className=r;h.appendChild(a);c.cancelEvent(i);a.onmousemove=h.ontouchmove=function(b){var o=n(b);if(j==\"h\")a.style.left=d.x+o+\"px\";else a.style.top=d.y+o+\"px\";c.cancelEvent(b)};a.onmouseup=h.ontouchend=function(b){if(a.parentNode!=null){a.parentNode.removeChild(a);s(n(b)); h.ontouchmove=null}}};";
	}
}
