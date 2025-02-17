What is JWt ?
------------

* JWt
  * 👀== Java library -- , via pure Java component-driven approach, for -- 
    * developing web applications 👀
    * renders either Ajax or plain HTML
  * vs JSF,
    * NO
      * exist concept of a page
      * split between page "views" -- & -- reusable "components" 
    * 💡ALL is a widget 💡
      * -> -- can be reused -- | OTHER widgets
  * see [JWT homepage](http://www.webtoolkit.eu/jwt)

Dependencies
------------

* requirements
  * Servlet container v2.5 or v3.0
  * if you want to use PDF rendering support (`WPdfImage` and `WPdfRenderer` classes) -> add [PdfJet](http://pdfjet.com/) | your project
* | deploy servlet v3.0 container,
  * & you use server push features -> able to use asynchronous I/O functionality / improve scalability 


Building
--------

* use ant

Demos, examples
---------------

* [examples/](examples)

Maven
-----

* ant build file has a target / -- generate -- maven pom files
  ```
  ant mvn
  ```
* if you want to install 2 artifacts | your local repository ->
  ```
  mvn install:install-file -Dfile=dist/jwt-3.3.2.jar -DpomFile=jwt-3.3.2.pom
  mvn install:install-file -Dfile=dist/jwt-auth-3.3.2.jar -DpomFile=jwt-auth-3.3.2.pom
  ```
* corresponding dependency blocks
  ```
  <dependency>
    <groupId>eu.webtoolkit</groupId>
    <artifactId>jwt</artifactId>
    <version>3.3.2</version>
  </dependency>

  <dependency>
    <groupId>javax.servlet</groupId>
    <artifactId>servlet-api</artifactId>
    <version>2.5</version>
  </dependency>
  ```
* OPTIONAL JWt dependencies
  ```
      <!-- optional, for JWT Auth -->
      <dependency>
        <groupId>eu.webtoolkit</groupId>
        <artifactId>jwt-auth</artifactId>
        <version>3.3.2</version>
      </dependency>
  
      <!-- optional, for PDF Rendering -->
      <dependency>
        <groupId>com.pdfjet</groupId>
        <artifactId>pdfjet</artifactId>
        <version>4.75</version>
      </dependency>
  
      <!-- optional, for CSS stylesheet support in XHTML renderer -->
      <dependency>
        <groupId>org.antlr</groupId>
        <artifactId>antlr4-runtime</artifactId>
        <version>4.7.2</version>
      </dependency>
  
      <!-- optional, for server-side WebGL fallback -->
      <dependency>
        <groupId>org.jogamp.jogl</groupId>
        <artifactId>jogl-all</artifactId>
        <version>2.0-rc11</version>
      </dependency>
  
      <!-- optional, for server-side WebGL fallback -->
      <dependency>
        <groupId>org.jogamp.gluegen</groupId>
        <artifactId>gluegen-rt-main</artifactId>
        <version>2.0-rc11</version>
      </dependency>
  
      <!-- may be needed if your J2EE container doesn't provide this -->
      <dependency>
        <groupId>org.apache.geronimo.javamail</groupId>
        <artifactId>geronimo-javamail_1.4_mail</artifactId>
        <version>1.8.1</version>
        <scope>provided</scope>
      </dependency>

  ```
