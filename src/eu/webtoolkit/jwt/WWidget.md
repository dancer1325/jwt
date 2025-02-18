* == 👀UI component's abstract base class 👀
  * implementations -- derive from --
    * `WWebWidget`
      * use cases
        * basic widgets / DIRECT HTML counter-part 
    * `WCompositeWidget`
      * use cases
        * REST of cases
  * provides methods -- to modify, based on CSS, the -- 
    * style
    * layout
* UI -- is organized in a -- tree structure /
  * EACH node == widget
  * ALL widgets, except application's root widget & dialogs, -- have a -- parent (NORMALLY `WContainerWidget`)
    * if you want to add a `WWidget` | `WContainerWidget` -> use
      * `WContainerWidget.addWidget(WWidget widget)`
      * `WContainerWidget.setLayout(WLayout layout)`
* TODO: