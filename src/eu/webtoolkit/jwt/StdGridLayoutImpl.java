/*
 * Copyright (C) 2009 Emweb bvba, Leuven, Belgium.
 *
 * See the LICENSE file for terms of use.
 */
package eu.webtoolkit.jwt;

import java.util.*;
import java.util.regex.*;
import java.io.*;
import java.lang.ref.*;
import java.util.concurrent.locks.ReentrantLock;
import javax.servlet.http.*;
import javax.servlet.*;
import eu.webtoolkit.jwt.*;
import eu.webtoolkit.jwt.chart.*;
import eu.webtoolkit.jwt.utils.*;
import eu.webtoolkit.jwt.servlet.*;

class StdGridLayoutImpl extends StdLayoutImpl {
	public StdGridLayoutImpl(WLayout layout, Grid grid) {
		super(layout);
		this.grid_ = grid;
		this.useFixedLayout_ = false;
		this.forceUpdate_ = false;
		String THIS_JS = "js/StdGridLayoutImpl.js";
		WApplication app = WApplication.getInstance();
		if (!app.isJavaScriptLoaded(THIS_JS)) {
			app.getStyleSheet().addRule("table.Wt-hcenter",
					"margin: 0px auto;position: relative");
			app.doJavaScript(wtjs1(app), false);
			app.doJavaScript(appjs1(app), false);
			app.setJavaScriptLoaded(THIS_JS);
			app.addAutoJavaScript(app.getJavaScriptClass()
					+ ".layouts.adjust();");
		}
	}

	public int getMinimumHeight() {
		final int colCount = this.grid_.columns_.size();
		final int rowCount = this.grid_.rows_.size();
		int total = 0;
		for (int i = 0; i < rowCount; ++i) {
			int minHeight = 0;
			for (int j = 0; j < colCount; ++j) {
				WLayoutItem item = this.grid_.items_.get(i).get(j).item_;
				if (item != null) {
					minHeight = Math.max(minHeight, getImpl(item)
							.getMinimumHeight());
				}
			}
			total += minHeight;
		}
		return total * (rowCount - 1) * this.grid_.verticalSpacing_;
	}

	public DomElement createDomElement(boolean fitWidth, boolean fitHeight,
			WApplication app) {
		this.forceUpdate_ = false;
		final int colCount = this.grid_.columns_.size();
		final int rowCount = this.grid_.rows_.size();
		boolean hasResizeHandles = false;
		for (int i = 0; i < colCount; ++i) {
			if (this.grid_.columns_.get(i).resizable_) {
				hasResizeHandles = true;
				break;
			}
		}
		if (!hasResizeHandles) {
			for (int i = 0; i < rowCount; ++i) {
				if (this.grid_.rows_.get(i).resizable_) {
					hasResizeHandles = true;
					break;
				}
			}
		}
		if (hasResizeHandles
				&& !app.isJavaScriptLoaded("js/StdGridLayoutImpl-resize.js")) {
			SizeHandle.loadJavaScript(app);
			app.doJavaScript(wtjs2(app), false);
			app.setJavaScriptLoaded("js/StdGridLayoutImpl-resize.js");
		}
		int totalColStretch = 0;
		for (int col = 0; col < colCount; ++col) {
			totalColStretch += Math.max(0,
					this.grid_.columns_.get(col).stretch_);
		}
		int totalRowStretch = 0;
		for (int row = 0; row < rowCount; ++row) {
			totalRowStretch += Math.max(0, this.grid_.rows_.get(row).stretch_);
		}
		int[] margin = { 0, 0, 0, 0 };
		if (this.getLayout().getParentLayout() == null) {
			margin[3] = this.getLayout().getContentsMargin(Side.Left);
			margin[0] = this.getLayout().getContentsMargin(Side.Top);
			margin[1] = this.getLayout().getContentsMargin(Side.Right);
			margin[2] = this.getLayout().getContentsMargin(Side.Bottom);
		}
		DomElement div = DomElement.createNew(DomElementType.DomElement_DIV);
		div.setId(this.getId());
		div.setProperty(Property.PropertyStylePosition, "relative");
		String divStyle = "";
		if (fitHeight && !app.getEnvironment().agentIsIElt(9)
				&& !app.getEnvironment().agentIsWebKit()) {
			divStyle += "height: 100%;";
		}
		if (app.getEnvironment().agentIsIElt(9)) {
			divStyle += "zoom: 1;";
		}
		if (divStyle.length() != 0) {
			div.setProperty(Property.PropertyStyle, divStyle);
		}
		DomElement table = DomElement
				.createNew(DomElementType.DomElement_TABLE);
		{
			String style = "";
			if (fitWidth) {
				if (this.useFixedLayout_) {
					style += "table-layout: fixed;";
				}
				style += "width: 100%;";
			}
			if (fitHeight) {
				if (!app.getEnvironment().hasAjax()) {
					style += "height: 100%;";
				}
			}
			table.setProperty(Property.PropertyStyle, style);
		}
		DomElement tbody = DomElement
				.createNew(DomElementType.DomElement_TBODY);
		if (fitWidth) {
			for (int col = 0; col < colCount; ++col) {
				DomElement c = DomElement
						.createNew(DomElementType.DomElement_COL);
				int stretch = Math
						.max(0, this.grid_.columns_.get(col).stretch_);
				if (stretch != 0 || fitWidth && totalColStretch == 0) {
					int pct = totalColStretch == 0 ? 100 / colCount : 100
							* stretch / totalColStretch;
					c.setProperty(Property.PropertyStyle, "width:"
							+ String.valueOf(pct) + "%;");
				}
				table.addChild(c);
				boolean resizeHandleRight = col < colCount - 1
						&& this.grid_.columns_.get(col).resizable_;
				if (resizeHandleRight) {
					c = DomElement.createNew(DomElementType.DomElement_COL);
					c.setProperty(Property.PropertyStyleWidth, String
							.valueOf(this.grid_.horizontalSpacing_)
							+ "px");
					c.setProperty(Property.PropertyClass, "Wt-vrh");
					table.addChild(c);
				}
			}
		}
		List<Boolean> overSpanned = new ArrayList<Boolean>();
		{
			int insertPos = 0;
			for (int ii = 0; ii < colCount * rowCount; ++ii)
				overSpanned.add(insertPos + ii, false);
		}
		;
		boolean resizeHandleAbove = false;
		int prevRowWithItem = -1;
		for (int row = 0; row < rowCount; ++row) {
			boolean resizeHandleBelow = row < rowCount - 1
					&& this.grid_.rows_.get(row).resizable_;
			DomElement tr = DomElement.createNew(DomElementType.DomElement_TR);
			String heightPct = "";
			int stretch = Math.max(0, this.grid_.rows_.get(row).stretch_);
			if (stretch != 0 || fitHeight && totalRowStretch == 0) {
				int pct = totalRowStretch == 0 ? 100 / rowCount : 100 * stretch
						/ totalRowStretch;
				StringWriter style = new StringWriter();
				style.append("height: ").append(String.valueOf(pct)).append(
						"%;");
				heightPct = style.toString();
				tr.setProperty(Property.PropertyStyle, heightPct);
			}
			boolean resizeHandleLeft = false;
			int prevColumnWithItem = -1;
			boolean rowVisible = false;
			for (int col = 0; col < colCount; ++col) {
				boolean resizeHandleRight = col < colCount - 1
						&& this.grid_.columns_.get(col - 1
								+ this.grid_.items_.get(row).get(col).colSpan_).resizable_;
				if (!overSpanned.get(row * colCount + col)) {
					Grid.Item item = this.grid_.items_.get(row).get(col);
					boolean itemFitWidth = item.colSpan_ == (int) colCount
							|| totalColStretch == 0;
					boolean itemFitHeight = item.rowSpan_ == (int) rowCount
							|| totalRowStretch == 0;
					int colSpan = 0;
					for (int i = 0; i < item.rowSpan_; ++i) {
						if (this.grid_.rows_.get(row + i).stretch_ != 0) {
							itemFitHeight = true;
						} else {
							if (!(stretch != 0)
									&& !(item.item_ != null && item.item_
											.getLayout() != null)) {
								itemFitHeight = false;
							}
						}
						colSpan = item.colSpan_;
						for (int j = 0; j < item.colSpan_; ++j) {
							if (this.grid_.columns_.get(col + j).stretch_ != 0) {
								itemFitWidth = true;
							}
							if (i + j > 0) {
								overSpanned.set((row + i) * colCount + col + j,
										true);
							}
							if (j + 1 < item.colSpan_
									&& this.grid_.columns_.get(col + j).resizable_) {
								++colSpan;
							}
						}
					}
					AlignmentFlag hAlign = EnumUtils.enumFromSet(EnumUtils
							.mask(item.alignment_,
									AlignmentFlag.AlignHorizontalMask));
					AlignmentFlag vAlign = EnumUtils.enumFromSet(EnumUtils
							.mask(item.alignment_,
									AlignmentFlag.AlignVerticalMask));
					if (hAlign != null && hAlign != AlignmentFlag.AlignJustify) {
						itemFitWidth = false;
					}
					if (vAlign != null) {
						itemFitHeight = false;
					}
					int[] padding = { 0, 0, 0, 0 };
					boolean itemVisible = this.hasItem(row, col);
					rowVisible = rowVisible || itemVisible;
					if (itemVisible) {
						int nextRow = this.nextRowWithItem(row, col);
						int prevRow = prevRowWithItem;
						int nextCol = this.nextColumnWithItem(row, col);
						int prevCol = prevColumnWithItem;
						if (prevRow == -1) {
							padding[0] = margin[0];
						} else {
							if (!resizeHandleAbove) {
								padding[0] = (this.grid_.verticalSpacing_ + 1) / 2;
							}
						}
						if (nextRow == (int) rowCount) {
							padding[2] = margin[2];
						} else {
							if (!resizeHandleBelow) {
								padding[2] = this.grid_.verticalSpacing_ / 2;
							}
						}
						if (prevCol == -1) {
							padding[3] = margin[3];
						} else {
							if (!resizeHandleLeft) {
								padding[3] = (this.grid_.horizontalSpacing_ + 1) / 2;
							}
						}
						if (nextCol == (int) colCount) {
							padding[1] = margin[1];
						} else {
							if (!resizeHandleRight) {
								padding[1] = this.grid_.horizontalSpacing_ / 2;
							}
						}
					}
					DomElement td = DomElement
							.createNew(DomElementType.DomElement_TD);
					if (app.getEnvironment().agentIsIElt(9)) {
						td.setProperty(Property.PropertyStylePosition,
								"relative");
					}
					if (item.item_ != null) {
						DomElement c = getImpl(item.item_).createDomElement(
								itemFitWidth, itemFitHeight, app);
						if (hAlign == null) {
							hAlign = AlignmentFlag.AlignJustify;
						}
						switch (hAlign) {
						case AlignCenter: {
							DomElement itable = DomElement
									.createNew(DomElementType.DomElement_TABLE);
							itable.setProperty(Property.PropertyClass,
									"Wt-hcenter");
							if (vAlign == null) {
								itable.setProperty(Property.PropertyStyle,
										"height:100%;");
							}
							DomElement irow = DomElement
									.createNew(DomElementType.DomElement_TR);
							DomElement itd = DomElement
									.createNew(DomElementType.DomElement_TD);
							if (vAlign == null) {
								itd.setProperty(Property.PropertyStyle,
										"height:100%;");
							}
							itd.addChild(c);
							irow.addChild(itd);
							itable.addChild(irow);
							c = itable;
							break;
						}
						case AlignRight:
							if (!c.isDefaultInline()) {
								c.setProperty(Property.PropertyStyleFloat,
										"right");
							} else {
								td.setProperty(Property.PropertyStyleTextAlign,
										"right");
							}
							break;
						case AlignLeft:
							if (!c.isDefaultInline()) {
								c.setProperty(Property.PropertyStyleFloat,
										"left");
							} else {
								td.setProperty(Property.PropertyStyleTextAlign,
										"left");
							}
							break;
						case AlignJustify:
							if (c.getProperty(Property.PropertyStyleWidth)
									.length() == 0
									&& this.useFixedLayout_
									&& !app.getEnvironment().agentIsWebKit()
									&& !app.getEnvironment().agentIsGecko()
									&& !c.isDefaultInline()) {
								c.setProperty(Property.PropertyStyleWidth,
										"100%");
							}
							break;
						default:
							break;
						}
						if (!app.getEnvironment().agentIsIElt(9)) {
							c.setProperty(Property.PropertyStyleBoxSizing,
									"border-box");
						}
						if (c.getType() == DomElementType.DomElement_BUTTON) {
							c
									.setProperty(
											Property.PropertyStyleMarginLeft,
											"0");
							c.setProperty(Property.PropertyStyleMarginRight,
									"0");
						}
						td.addChild(c);
					}
					{
						String style = "";
						if (vAlign == null) {
							style += heightPct;
						}
						if (app.getLayoutDirection() == LayoutDirection.RightToLeft) {
							int tmp = padding[1];
							padding[1] = padding[3];
							padding[3] = tmp;
							;
						}
						if (padding[0] == padding[1]
								&& padding[0] == padding[2]
								&& padding[0] == padding[3]) {
							if (padding[0] != 0) {
								style += "padding:"
										+ String.valueOf(padding[0]) + "px;";
							}
						} else {
							style += "padding:" + String.valueOf(padding[0])
									+ "px " + String.valueOf(padding[1])
									+ "px " + String.valueOf(padding[2])
									+ "px " + String.valueOf(padding[3])
									+ "px;";
						}
						if (vAlign != null) {
							switch (vAlign) {
							case AlignTop:
								style += "vertical-align:top;";
								break;
							case AlignMiddle:
								style += "vertical-align:middle;";
								break;
							case AlignBottom:
								style += "vertical-align:bottom;";
							default:
								break;
							}
						}
						if (style.length() != 0) {
							td.setProperty(Property.PropertyStyle, style);
						}
					}
					if (item.rowSpan_ != 1) {
						td.setProperty(Property.PropertyRowSpan, String
								.valueOf(item.rowSpan_));
					}
					if (colSpan != 1) {
						td.setProperty(Property.PropertyColSpan, String
								.valueOf(colSpan));
					}
					tr.addChild(td);
					if (itemVisible && resizeHandleRight) {
						td = DomElement.createNew(DomElementType.DomElement_TD);
						td.setProperty(Property.PropertyClass, "Wt-vrh");
						String style = "padding:" + String.valueOf(padding[0])
								+ "px 0px" + String.valueOf(padding[2]) + "px;";
						td.setProperty(Property.PropertyStyle, style);
						DomElement div2 = DomElement
								.createNew(DomElementType.DomElement_DIV);
						div2.setProperty(Property.PropertyStyleWidth, String
								.valueOf(this.grid_.horizontalSpacing_)
								+ "px");
						td.addChild(div2);
						tr.addChild(td);
					}
					if (itemVisible) {
						resizeHandleLeft = resizeHandleRight;
						prevColumnWithItem = col;
					}
				}
			}
			tbody.addChild(tr);
			if (rowVisible) {
				if (resizeHandleBelow) {
					tr = DomElement.createNew(DomElementType.DomElement_TR);
					tr.setProperty(Property.PropertyClass, "Wt-hrh");
					String height = String.valueOf(this.grid_.verticalSpacing_)
							+ "px";
					tr.setProperty(Property.PropertyStyleHeight, height);
					DomElement td = DomElement
							.createNew(DomElementType.DomElement_TD);
					td.setProperty(Property.PropertyColSpan, String
							.valueOf(colCount));
					String style2 = "padding: 0px" + String.valueOf(margin[1])
							+ "px 0px" + String.valueOf(margin[3]) + "px;";
					td.setProperty(Property.PropertyStyleHeight, style2);
					DomElement div2 = DomElement
							.createNew(DomElementType.DomElement_DIV);
					div2.setProperty(Property.PropertyStyleHeight, height);
					td.addChild(div2);
					tr.addChild(td);
					tbody.addChild(tr);
				}
				prevRowWithItem = row;
				resizeHandleAbove = resizeHandleBelow;
			}
		}
		table.addChild(tbody);
		if (fitHeight) {
			StringWriter layoutAdd = new StringWriter();
			layoutAdd.append(app.getJavaScriptClass()).append(
					".layouts.add(new Wt3_1_8.StdLayout( Wt3_1_8, '").append(
					div.getId()).append("', { stretch: [");
			for (int i = 0; i < rowCount; ++i) {
				if (i != 0) {
					layoutAdd.append(",");
				}
				int stretch = 0;
				if (totalRowStretch == 0 && fitHeight) {
					stretch = 1;
				} else {
					stretch = this.grid_.rows_.get(i).stretch_;
				}
				layoutAdd.append(String.valueOf(stretch));
			}
			layoutAdd.append("], minheight: [");
			for (int i = 0; i < rowCount; ++i) {
				if (i != 0) {
					layoutAdd.append(",");
				}
				int minHeight = 0;
				for (int j = 0; j < colCount; ++j) {
					WLayoutItem item = this.grid_.items_.get(i).get(j).item_;
					if (item != null) {
						minHeight = Math.max(minHeight, getImpl(item)
								.getMinimumHeight());
					}
				}
				if (i == 0) {
					minHeight += margin[0];
				} else {
					minHeight += this.grid_.verticalSpacing_;
				}
				if (i == rowCount - 1) {
					minHeight += margin[2];
				}
				layoutAdd.append(String.valueOf(minHeight));
			}
			layoutAdd.append("]}));");
			table.callJavaScript(layoutAdd.toString());
		}
		div.addChild(table);
		return div;
	}

	public void updateDom() {
		if (this.forceUpdate_) {
			this.forceUpdate_ = false;
			WApplication app = WApplication.getInstance();
			app.doJavaScript(app.getJavaScriptClass() + ".layouts.adjust('"
					+ this.getId() + "');");
		}
	}

	public void setHint(String name, String value) {
		if (name.equals("table-layout")) {
			if (value.equals("fixed")) {
				this.useFixedLayout_ = true;
			} else {
				if (value.equals("auto")) {
					this.useFixedLayout_ = false;
				} else {
					WApplication.getInstance().log("error").append(
							"WGridLayout: unrecognized hint value '").append(
							value).append("' for '").append(name).append("'");
				}
			}
		} else {
			WApplication.getInstance().log("error").append(
					"WGridLayout: unrecognized hint '").append(name)
					.append("'");
		}
	}

	public static String getChildrenResizeJS() {
		String THIS_JS = "js/WtResize.js";
		WApplication app = WApplication.getInstance();
		if (!app.isJavaScriptLoaded(THIS_JS)) {
			app.doJavaScript(wtjs10(app), false);
			app.setJavaScriptLoaded(THIS_JS);
		}
		return "Wt3_1_8.ChildrenResize";
	}

	public boolean itemResized(WLayoutItem item) {
		WWidget ww = item.getWidget();
		if (ww != null && ww.getJavaScriptMember("wtResize").length() != 0) {
			this.forceUpdate_ = true;
			return true;
		}
		final int colCount = this.grid_.columns_.size();
		final int rowCount = this.grid_.rows_.size();
		for (int row = 0; row < rowCount; ++row) {
			if (this.grid_.rows_.get(row).stretch_ <= 0) {
				for (int col = 0; col < colCount; ++col) {
					if (this.grid_.items_.get(row).get(col).item_ == item) {
						this.forceUpdate_ = true;
						return true;
					}
				}
			}
		}
		return false;
	}

	void containerAddWidgets(WContainerWidget container) {
		super.containerAddWidgets(container);
		if (!(container != null)) {
			return;
		}
		WApplication app = WApplication.getInstance();
		if (this.getParentLayoutImpl() == null) {
			if (container == app.getRoot()) {
				app.setBodyClass(app.getBodyClass() + " Wt-layout");
				app.setHtmlClass(app.getHtmlClass() + " Wt-layout");
			}
		}
	}

	private Grid grid_;
	private boolean useFixedLayout_;
	private boolean forceUpdate_;

	private int nextRowWithItem(int row, int c) {
		for (row += this.grid_.items_.get(row).get(c).rowSpan_; row < (int) this.grid_.rows_
				.size(); ++row) {
			for (int col = 0; col < this.grid_.columns_.size(); col += this.grid_.items_
					.get(row).get(col).colSpan_) {
				if (this.hasItem(row, col)) {
					return row;
				}
			}
		}
		return this.grid_.rows_.size();
	}

	private int nextColumnWithItem(int row, int col) {
		for (;;) {
			col = col + this.grid_.items_.get(row).get(col).colSpan_;
			if (col < (int) this.grid_.columns_.size()) {
				for (int i = 0; i < this.grid_.rows_.size(); ++i) {
					if (this.hasItem(i, col)) {
						return col;
					}
				}
			} else {
				return this.grid_.columns_.size();
			}
		}
	}

	private boolean hasItem(int row, int col) {
		WLayoutItem item = this.grid_.items_.get(row).get(col).item_;
		if (item != null) {
			WWidget w = item.getWidget();
			return !(w != null) || !w.isHidden();
		} else {
			return false;
		}
	}

	static String wtjs1(WApplication app) {
		String s = "function(a,t,j){var o=this;this.getId=function(){return t};this.WT=a;this.marginH=function(b){var d=b.parentNode,e=0;if(!a.boxSizing(b)){e=a.px(b,\"marginLeft\");e+=a.px(b,\"marginRight\");e+=a.px(b,\"borderLeftWidth\");e+=a.px(b,\"borderRightWidth\");e+=a.px(b,\"paddingLeft\");e+=a.px(b,\"paddingRight\")}e+=a.pxself(d,\"paddingLeft\");e+=a.pxself(d,\"paddingRight\");return e};this.marginV=function(b){var d=a.px(b,\"marginTop\");d+=a.px(b,\"marginBottom\");if(!a.boxSizing(b)){d+= a.px(b,\"borderTopWidth\");d+=a.px(b,\"borderBottomWidth\");d+=a.px(b,\"paddingTop\");d+=a.px(b,\"paddingBottom\")}return d};this.getColumn=function(b){var d,e,i,c=a.getElement(t).firstChild.childNodes;e=d=0;for(i=c.length;e<i;e++){var f=c[e];if(a.hasTag(f,\"COLGROUP\")){e=-1;c=f.childNodes;i=c.length}if(a.hasTag(f,\"COL\"))if(f.className!=\"Wt-vrh\")if(d==b)return f;else++d}return null};this.adjustCell=function(b,d,e){var i=d==0;d-=a.pxself(b,\"paddingTop\");d-=a.pxself(b,\"paddingBottom\");if(d<=0)d=0;b.style.height= d+\"px\";if(!(b.style.verticalAlign||b.childNodes.length==0)){var c=b.childNodes[0];if(d<=0)d=0;if(c.className==\"Wt-hcenter\"){c.style.height=d+\"px\";c=c.firstChild.firstChild;if(!a.hasTag(c,\"TD\"))c=c.firstChild;if(c.style.height!=d+\"px\")c.style.height=d+\"px\";c=c.firstChild}if(b.childNodes.length==1)d-=this.marginV(c);if(d<=0)d=0;if(!a.hasTag(c,\"TABLE\"))if(!i&&c.wtResize){b=c.parentNode.offsetWidth-o.marginH(c);e=o.getColumn(e).style.width;i=e.length;if(i>0&&e[i-1]==\"%\"){c.style.position=\"absolute\";c.style.width= b+\"px\"}c.wtResize(c,b,d)}else if(c.style.height!=d+\"px\"){c.style.height=d+\"px\";if(c.className==\"Wt-wrapdiv\")if(a.isIE&&a.hasTag(c.firstChild,\"TEXTAREA\"))c.firstChild.style.height=d-a.pxself(c,\"marginBottom\")+\"px\"}}};this.adjustRow=function(b,d){var e=[];if(b.style.height!=d+\"px\")b.style.height=d+\"px\";b=b.childNodes;var i,c,f,h;i=0;h=-1;for(c=b.length;i<c;++i){f=b[i];f.className!=\"Wt-vrh\"&&++h;if(f.rowSpan!=1){this.adjustCell(f,0,h);e.push({td:f,col:h})}else this.adjustCell(f,d,h)}return e};this.adjust= function(){var b=a.getElement(t);if(!b)return false;o.initResize&&o.initResize(a,t,j);if(a.isHidden(b))return true;var d=b.firstChild,e=b.parentNode;if(d.style.height!==\"\")d.style.height=\"\";var i=a.pxself(e,\"height\"),c=false;if(i===0){c=true;i=e.clientHeight}var f=e.clientWidth;if(!(b.dirty||d.w!==f||d.h!==i))return true;d.w=f;d.h=i;b.dirty=null;var h=i;if(c){h-=a.px(e,\"paddingTop\");h-=a.px(e,\"paddingBottom\")}else if(a.boxSizing(e)){h-=a.px(e,\"borderTopWidth\");h-=a.px(e,\"borderBottomWidth\");h-=a.px(e, \"paddingTop\");h-=a.px(e,\"paddingBottom\")}h-=a.px(b,\"marginTop\");h-=a.px(b,\"marginBottom\");if(e.children&&e.children.length!==1){c=0;for(i=e.children.length;c<i;++c){f=e.children[c];if(f!==b)h-=$(f).outerHeight()}}b=e=0;var p,n;i=d.rows.length;for(p=c=0;c<i;c++){f=d.rows[c];if(f.className===\"Wt-hrh\")h-=f.offsetHeight;else{b+=j.minheight[p];if(j.stretch[p]<=0)h-=f.offsetHeight;else e+=j.stretch[p];++p}}h=h>b?h:b;b=[];if(e!==0&&h>0){var r=h,s;for(p=c=0;c<i;c++){f=d.rows[c];if(f.className!==\"Wt-hrh\"){n= j.stretch[p];if(n!==0){if(n!==-1){s=j.minheight[p];n=h*n/e;n=r>n?n:r;n=Math.round(s>n?s:n);r-=n}else n=f.offsetHeight;a.addAll(b,this.adjustRow(f,n))}++p}}}c=0;for(i=b.length;c<i;++c){f=b[c].td;h=b[c].col;this.adjustCell(f,f.offsetHeight,h)}if(d.style.tableLayout!==\"fixed\")return true;p=0;r=d.childNodes;var y,A,z;b=0;for(e=r.length;b<e;b++){h=r[b];if(a.hasTag(h,\"COLGROUP\")){b=-1;r=h.childNodes;e=r.length}if(a.hasTag(h,\"COL\")){if(a.pctself(h,\"width\")===0){c=s=0;for(i=d.rows.length;c<i;c++){f=d.rows[c]; n=f.childNodes;A=y=0;for(z=n.length;A<z;A++){f=n[A];if(f.colSpan===1&&y===p&&f.childNodes.length===1){f=f.firstChild;f=f.offsetWidth+o.marginH(f);s=Math.max(s,f);break}y+=f.colSpan;if(y>p)break}}if(s>0&&a.pxself(h,\"width\")!==s)h.style.width=s+\"px\"}++p}}return true};this.contains=function(b){var d=a.getElement(t);b=a.getElement(b.getId());return d&&b?a.contains(d,b):false}}";
		if ("ctor.StdLayout".indexOf(".prototype") != -1) {
			return "Wt3_1_8.ctor.StdLayout = " + s + ";";
		} else {
			if ("ctor.StdLayout".substring(0, 5).compareTo(
					"ctor.".substring(0, 5)) == 0) {
				return "Wt3_1_8." + "ctor.StdLayout".substring(5) + " = " + s
						+ ";";
			} else {
				return "Wt3_1_8.ctor.StdLayout = function() { (" + s
						+ ").apply(Wt3_1_8, arguments) };";
			}
		}
	}

	static String wtjs2(WApplication app) {
		String s = "function(a,t,j){function o(g,k){if(g.offsetWidth>0)return g.offsetWidth;else{g=n.firstChild.rows[0];var l,m,q,u;q=m=0;for(u=g.childNodes.length;m<u;++m){l=g.childNodes[m];if(l.className!=\"Wt-vrh\"){if(q==k)return l.offsetWidth;q+=l.colSpan}}return 0}}function b(g,k){var l=a.getElement(t).firstChild;p(g).style.width=k+\"px\";var m,q,u,w;q=m=0;for(u=l.rows.length;m<u;m++){w=l.rows[m];if(w.className!=\"Wt-hrh\"){var v,B,C,F;C=B=0;for(F=w.childNodes.length;B< F;++B){v=w.childNodes[B];if(v.className!=\"Wt-vrh\"){if(v.colSpan==1&&C==g&&v.childNodes.length==1){w=v.firstChild;v=k-h.marginH(w);w.style.width=v+\"px\";break}C+=v.colSpan}}++q}}}function d(g,k,l){var m=g.firstChild;new a.SizeHandle(a,\"v\",m.offsetHeight,m.offsetWidth,-g.parentNode.previousSibling.offsetHeight,g.parentNode.nextSibling.offsetHeight,\"Wt-vsh\",function(q){i(g,k,q)},m,n,l,0,0)}function e(g,k,l){var m=-g.previousSibling.offsetWidth,q=g.nextSibling.offsetWidth,u=g.firstChild,w=a.pxself(r.rows[0].childNodes[0], \"paddingTop\"),v=a.pxself(r.rows[r.rows.length-1].childNodes[0],\"paddingBottom\");v=r.offsetHeight-w-v;if($(document.body).hasClass(\"Wt-rtl\")){var B=m;m=-q;q=-B}new a.SizeHandle(a,\"h\",u.offsetWidth,v,m,q,\"Wt-hsh\",function(C){f(g,k,C)},u,n,l,0,-g.offsetTop+w-a.pxself(g,\"paddingTop\"))}function i(g,k,l){var m=g.parentNode.previousSibling;g=g.parentNode.nextSibling;var q=m.offsetHeight,u=g.offsetHeight;if(j.stretch[k]>0&&j.stretch[k+1]>0)j.stretch[k]=-1;if(j.stretch[k+1]==0)j.stretch[k+1]=-1;j.stretch[k]<= 0&&h.adjustRow(m,q+l);j.stretch[k+1]<=0&&h.adjustRow(g,u-l);a.getElement(t).dirty=true;window.onresize()}function c(){var g,k=0;for(g=0;;++g){var l=p(g);if(l)k+=a.pctself(l,\"width\");else break}if(k!=0)for(g=0;;++g)if(l=p(g)){var m=a.pctself(l,\"width\");if(m)l.style.width=m*100/k+\"%\"}else break}function f(g,k,l){g=p(k);var m=o(g,k),q=p(k+1),u=o(q,k+1);if($(document.body).hasClass(\"Wt-rtl\"))l=-l;if(a.pctself(g,\"width\")>0&&a.pctself(q,\"width\")>0){g.style.width=\"\";c()}a.pctself(g,\"width\")==0&&b(k,m+l); a.pctself(q,\"width\")==0&&b(k+1,u-l);window.onresize()}var h=this,p=h.getColumn,n=a.getElement(t);if(n)if(!h.resizeInitialized){var r=n.firstChild,s,y,A,z;y=s=0;for(A=r.rows.length;s<A;s++){z=r.rows[s];if(z.className==\"Wt-hrh\"){var x=z.firstChild;x.ri=y-1;x.onmousedown=x.ontouchstart=function(g){d(this,this.ri,g||window.event)}}else{var D,E,G;E=D=0;for(G=z.childNodes.length;D<G;++D){x=z.childNodes[D];if(x.className==\"Wt-vrh\"){x.ci=E-1;x.onmousedown=x.ontouchstart=function(g){e(this,this.ci,g||window.event)}}else E+= x.colSpan}++y}}h.resizeInitialized=true}}";
		if ("StdLayout.prototype.initResize".indexOf(".prototype") != -1) {
			return "Wt3_1_8.StdLayout.prototype.initResize = " + s + ";";
		} else {
			if ("StdLayout.prototype.initResize".substring(0, 5).compareTo(
					"ctor.".substring(0, 5)) == 0) {
				return "Wt3_1_8."
						+ "StdLayout.prototype.initResize".substring(5) + " = "
						+ s + ";";
			} else {
				return "Wt3_1_8.StdLayout.prototype.initResize = function() { ("
						+ s + ").apply(Wt3_1_8, arguments) };";
			}
		}
	}

	static String appjs1(WApplication app) {
		return app.getJavaScriptClass()
				+ ".layouts = new (function(){var a=[],t=false;this.add=function(j){var o,b;o=0;for(b=a.length;o<b;++o){var d=a[o];if(d.getId()==j.getId()){a[o]=j;return}else if(j.contains(d)){a.splice(o,0,j);return}}a.push(j)};this.adjust=function(j){if(j){if(j=$(\"#\"+j).get(0))j.dirty=true}else if(!t){t=true;for(var o=0;o<a.length;++o){j=a[o];if(!j.adjust()){j.WT.arrayRemove(a,o);--o}}t=false}}});";
	}

	static String wtjs10(WApplication app) {
		String s = "function(b,d,c){var a,f,e;a=this;b.style.height=c+\"px\";if(a.boxSizing(b)){c-=a.px(b,\"marginTop\");c-=a.px(b,\"marginBottom\");c-=a.px(b,\"borderTopWidth\");c-=a.px(b,\"borderBottomWidth\");c-=a.px(b,\"paddingTop\");c-=a.px(b,\"paddingBottom\");d-=a.px(b,\"marginLeft\");d-=a.px(b,\"marginRight\");d-=a.px(b,\"borderLeftWidth\");d-=a.px(b,\"borderRightWidth\");d-=a.px(b,\"paddingLeft\");d-=a.px(b,\"paddingRight\")}var g=c+\"px\";a=0;for(f=b.childNodes.length;a<f;++a){e=b.childNodes[a]; if(e.nodeType==1)if(e.wtResize)e.wtResize(e,d,c);else if(e.style.height!=g)e.style.height=g}}";
		if ("ChildrenResize".indexOf(".prototype") != -1) {
			return "Wt3_1_8.ChildrenResize = " + s + ";";
		} else {
			if ("ChildrenResize".substring(0, 5).compareTo(
					"ctor.".substring(0, 5)) == 0) {
				return "Wt3_1_8." + "ChildrenResize".substring(5) + " = " + s
						+ ";";
			} else {
				return "Wt3_1_8.ChildrenResize = function() { (" + s
						+ ").apply(Wt3_1_8, arguments) };";
			}
		}
	}
}
