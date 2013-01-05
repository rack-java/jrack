package com.zzzhc.web;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.reflections.Reflections;
import org.reflections.util.ConfigurationBuilder;

import com.zzzhc.rack.RequestMethod;
import com.zzzhc.routing.ICondition;
import com.zzzhc.routing.Route;
import com.zzzhc.routing.RouteSet;
import com.zzzhc.routing.annotation.Delete;
import com.zzzhc.routing.annotation.Get;
import com.zzzhc.routing.annotation.Head;
import com.zzzhc.routing.annotation.Path;
import com.zzzhc.routing.annotation.Post;
import com.zzzhc.routing.annotation.Put;
import com.zzzhc.routing.annotation.Resource;
import com.zzzhc.routing.annotation.Resources;
import com.zzzhc.routing.condition.MultiCondition;
import com.zzzhc.routing.condition.RegexPathCondition;
import com.zzzhc.routing.condition.RequestMethodCondition;

public class RouteSetBuilder {

	private static final String[] RESOURCES_ACTIONS = new String[] { "index",
			"new", "create", "show", "edit", "update", "destroy" };

	private static final String[] RESOURCE_ACTIONS = new String[] { "new",
			"create", "show", "edit", "update", "destroy" };

	private ITemplateEngine templateEngine;
	private ClassLoader loader;

	private List<String> packages = new ArrayList<String>();

	public RouteSetBuilder(ITemplateEngine templateEngine, ClassLoader loader) {
		this.templateEngine = templateEngine;
		this.loader = loader;
	}

	public RouteSet build() {
		RouteSet routeSet = new RouteSet();
		handlePackages(routeSet);
		return routeSet;
	}

	public RouteSetBuilder addPackage(Class<?> klass) {
		String packageName = klass.getName().replaceFirst("\\.[^.]+$", "");
		packages.add(packageName);
		return this;
	}

	private void handlePackages(RouteSet routeSet) {
		ConfigurationBuilder builder = ConfigurationBuilder.build(packages,
				loader);

		Reflections reflections = new Reflections(builder);

		Set<String> subTypes = reflections.getStore().getSubTypesOf(
				BaseController.class.getName());
		for (String klass : subTypes) {
			try {
				Class<?> c = loader.loadClass(klass);
				addClass(routeSet, c);
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
		}
	}

	private RouteSetBuilder addClass(RouteSet routeSet, Class<?> klass) {
		handleResources(routeSet, klass);
		handleResource(routeSet, klass);
		handleNormalClass(routeSet, klass);
		return this;
	}

	private void handleNormalClass(RouteSet routeSet, Class<?> klass) {
		Path path = klass.getAnnotation(Path.class);
		if (path == null) {
			return;
		}
		Method[] methods = klass.getMethods();
		for (Method method : methods) {
			Path methodPath = method.getAnnotation(Path.class);
			String uri = "/";
			if (path.value().length() > 0) {
				uri += path.value() + "/";
			}
			if (methodPath == null) {
				uri += underscore(method.getName());
			} else {
				uri += methodPath.value();
			}

			for (Annotation a : method.getAnnotations()) {
				RequestMethod requestMethod = toRequestMethod(a);
				MultiCondition condition = new MultiCondition();
				condition
						.addCondition(new RequestMethodCondition(requestMethod));
				condition.addCondition(new RegexPathCondition(uri));

				addRoute(routeSet, klass, method, condition);
			}
		}
	}

	private RequestMethod toRequestMethod(Annotation a) {
		String name = a.getClass().getSimpleName().toUpperCase();
		try {
			return RequestMethod.valueOf(name);
		} catch (Exception e) {
			return null;
		}
	}

	private int indexOfAction(String[] actions, String action) {
		for (int i = 0; i < actions.length; i++) {
			if (actions[i].equals(action)) {
				return i;
			}
		}
		return -1;
	}

	private void handleResource(RouteSet routeSet, Class<?> klass) {
		Resource resource = klass.getAnnotation(Resource.class);
		if (resource == null) {
			return;
		}
		String name = resource.value();
		if ("".equals(name)) {
			name = klass.getSimpleName().replaceFirst("Controller$", "");
			name = underscore(name);
		}
		String pathPrefix = "/" + name;
		Method[] methods = klass.getMethods();

		ArrayList<Method> resourceMethods = new ArrayList<Method>();
		for (Method method : methods) {
			if (indexOfAction(RESOURCE_ACTIONS, method.getName()) != -1) {
				resourceMethods.add(method);
			}
		}

		for (Method method : resourceMethods) {
			String n = method.getName();
			ICondition condition = null;
			if (n.equals("new")) {
				condition = buildActionCondition(RequestMethod.GET, pathPrefix
						+ "/new");
			} else if (n.equals("create")) {
				condition = buildActionCondition(RequestMethod.POST, pathPrefix
						+ "/?");
			} else if (n.equals("show")) {
				condition = buildActionCondition(RequestMethod.GET, pathPrefix
						+ "/?");
			} else if (n.equals("edit")) {
				condition = buildActionCondition(RequestMethod.GET, pathPrefix
						+ "/edit");
			} else if (n.equals("update")) {
				condition = buildActionCondition(RequestMethod.PUT, pathPrefix
						+ "/?");
			} else if (n.equals("destroy")) {
				condition = buildActionCondition(RequestMethod.DELETE,
						pathPrefix + "/?");
			} else {
				continue;
			}
			addRoute(routeSet, klass, method, condition);
		}
	}

	private void handleResources(RouteSet routeSet, Class<?> klass) {
		Resources resources = klass.getAnnotation(Resources.class);
		if (resources == null) {
			return;
		}
		String name = resources.value();
		if ("".equals(name)) {
			name = klass.getSimpleName().replaceFirst("Controller$", "");
			name = underscore(name);
		}
		String pathPrefix = "/" + name;
		Method[] methods = klass.getMethods();

		for (String collection : resources.collection()) {
			Method method = findMethod(methods, collection);
			if (method != null) {
				RequestMethod requestMethod = getRequestMethod(method,
						RequestMethod.GET);
				MultiCondition condition = buildActionCondition(requestMethod,
						pathPrefix + "/" + collection + "/?");

				addRoute(routeSet, klass, method, condition);
			}
		}

		for (String member : resources.member()) {
			Method method = findMethod(methods, member);
			if (method != null) {
				RequestMethod requestMethod = getRequestMethod(method,
						RequestMethod.GET);

				MultiCondition condition = buildActionCondition(requestMethod,
						pathPrefix + "/([^/]+)/" + member + "/?");
				addRoute(routeSet, klass, method, condition);
			}
		}

		ArrayList<Method> resourcesMethods = new ArrayList<Method>();
		for (Method method : methods) {
			if (indexOfAction(RESOURCES_ACTIONS, method.getName()) != -1) {
				resourcesMethods.add(method);
			}
		}

		for (Method method : resourcesMethods) {
			String n = method.getName();
			ICondition condition = null;
			if (n.equals("index")) {
				condition = buildActionCondition(RequestMethod.GET, pathPrefix
						+ "/?");
			} else if (n.equals("new")) {
				condition = buildActionCondition(RequestMethod.GET, pathPrefix
						+ "/new");
			} else if (n.equals("create")) {
				condition = buildActionCondition(RequestMethod.POST, pathPrefix
						+ "/?");
			} else if (n.equals("show")) {
				condition = buildActionCondition(RequestMethod.GET, pathPrefix
						+ "/([^/]+)/?");
			} else if (n.equals("edit")) {
				condition = buildActionCondition(RequestMethod.GET, pathPrefix
						+ "/([^/]+)/edit");
			} else if (n.equals("update")) {
				condition = buildActionCondition(RequestMethod.PUT, pathPrefix
						+ "/([^/]+)/?");
			} else if (n.equals("destroy")) {
				condition = buildActionCondition(RequestMethod.DELETE,
						pathPrefix + "/([^/]+)/?");
			} else {
				continue;
			}
			addRoute(routeSet, klass, method, condition);
		}
	}

	private void addRoute(RouteSet routeSet, Class<?> klass, Method method,
			ICondition condition) {
		Action action = new Action(klass, method, templateEngine);
		routeSet.addRoute(new Route(condition, action));
	}

	private RequestMethod getRequestMethod(Method method,
			RequestMethod defaultMethod) {
		if (method.getAnnotation(Get.class) != null) {
			return RequestMethod.GET;
		} else if (method.getAnnotation(Post.class) != null) {
			return RequestMethod.POST;
		} else if (method.getAnnotation(Put.class) != null) {
			return RequestMethod.PUT;
		} else if (method.getAnnotation(Delete.class) != null) {
			return RequestMethod.DELETE;
		} else if (method.getAnnotation(Head.class) != null) {
			return RequestMethod.HEAD;
		}
		return defaultMethod;
	}

	private Method findMethod(Method[] methods, String name) {
		for (Method m : methods) {
			if (m.getName().equals(name)) {
				return m;
			}
		}
		return null;
	}

	private MultiCondition buildActionCondition(RequestMethod requestMethod,
			String regex) {
		MultiCondition multiCondition = new MultiCondition();
		multiCondition.addCondition(new RequestMethodCondition(requestMethod));
		multiCondition.addCondition(new RegexPathCondition(regex));
		return multiCondition;
	}

	private String underscore(String s) {
		return s.replaceAll("(.)([A-Z])", "$1_$2").toLowerCase();
	}

}
