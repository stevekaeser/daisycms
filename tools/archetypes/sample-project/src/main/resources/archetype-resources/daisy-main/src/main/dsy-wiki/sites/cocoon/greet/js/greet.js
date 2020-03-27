
cocoon.load("resource://org/outerj/daisy/frontend/util/daisy-util.js");
importClass(Packages.org.outerj.daisy.frontend.util.GenericPipeConfig);

var ctx;
try {
  ctx = cocoon.getComponent(Packages.org.outerj.daisy.frontend.components.SpringContextProvider.ROLE);
} finally {
  if (ctx) cocoon.releaseComponent(ctx);
}

var daisy = getDaisy();
var helloService = ctx.getBean("greetSample","helloService");

function greet() {
  var name = cocoon.parameters['name'];
  var greeting = helloService.greet(name);

  java.lang.System.out.println("greeting: " + greeting);

  var viewData = templatePipeViewData("jx/greeting.jx");
  viewData['greeting'] = greeting;
  cocoon.sendPage(daisy.getDaisyCocoonPath() + "/internal/genericPipe", viewData);
}

function templatePipeViewData(template) {
  var viewData = {};
  viewData['pageContext'] = daisy.getPageContext(daisy.getRepository());
  viewData['pipeConf'] = GenericPipeConfig.templatePipe(daisy.resolve(template));
  return viewData;
}

