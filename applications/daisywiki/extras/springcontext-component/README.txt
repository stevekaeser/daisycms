README
======

This is a component that delivers spring application contexts & spring beans to a Daisy environment.

USAGE
=====
Add this project as a dependency and use one of the following goals to deploy the component and its dependencies to the WEB-INF directory:
* daisy-maven-plugin:copy-wiki-deps
* maven-dependency-plugin:copy-dependecies 
* maven-dependency-plugin:copy

--------------
Configuration:
--------------

Place the component defintion in the daisy.xconf.
Alternatively you can add the component via the sitemap.xmap of your extensions.

<component class="org.outerj.daisy.frontend.components.SpringContextProviderImpl" 
           logger="springcontext" role="org.outerj.daisy.frontend.components.SpringContextProvider">     

        <classpathContext name="mySpringContext" src="/com/example/myproject/applicationContext.xml"/>

       	<fileContext name="somename" src="file:${daisywiki.data}/home/someplace/applicationContext.xml"/>

</component>

--------------------
Using the container:
--------------------

The SpringContextProvider just has one method to get spring beans. This is the getBean(appName, beanName) function.
If you wish to call this in a flow context you would do this like this:

var ctx = cocoon.getComponent(Packages.org.outerj.daisy.frontend.components.SpringContextProvider.ROLE);
return ctx.getBean("mySpringContext","The bean");

+ you can get access to the avalon servicemanager in you applicationContext like this:

<bean id="serviceManager" class="org.outerj.daisy.frontend.components.ServiceManagerFactoryBean"/>
