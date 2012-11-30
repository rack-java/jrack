package com.zzzhc.rack;

import java.io.File;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Server {

	public static final String DEFAULT_CONFIG_RU = "config.ru";

	private Logger logger = LoggerFactory.getLogger(Server.class);

	private String server = "HttpCore";
	private String config = DEFAULT_CONFIG_RU;

	private HandlerOptions handlerOptions = new HandlerOptions();

	public void start() throws Exception {
		Builder builder = Builder.load(new File(config));
		IMiddleware app = builder.toApp();
		IHandler handler = initializeHandler();
		handler.start(app, handlerOptions);
	}

	@SuppressWarnings("unchecked")
	private IHandler initializeHandler() {
		String className = "com.zzzhc.rack.handler."
				+ StringUtils.capitalize(server) + "Handler";
		try {
			Class<? extends IHandler> klass = (Class<? extends IHandler>) Class
					.forName(className);
			return klass.newInstance();
		} catch (ClassNotFoundException e) {
			logger.info("can't find server " + server + ", class(" + className
					+ ") not found");
			System.exit(1);
			throw new RackException(e);
		} catch (Exception e) {
			throw new RackException(e);
		}
	}

	public void parseOptions(String[] args) {
		Options options = buildOptions();
		CommandLineParser parser = new PosixParser();
		try {
			CommandLine cmd = parser.parse(options, args);
			if (cmd.hasOption('h')) {
				help(options);
			}
			if (cmd.hasOption('v')) {
				showVersion();
			}
			if (cmd.hasOption('s')) {
				server = cmd.getOptionValue('s');
			}
			if (cmd.hasOption('o')) {
				handlerOptions.setHost(cmd.getOptionValue('o'));
			}
			if (cmd.hasOption('p')) {
				handlerOptions
						.setPort(Integer.parseInt(cmd.getOptionValue('p')));
			}
			if (cmd.hasOption('O')) {
				String[] nvs = cmd.getOptionValues('O');
				for (String nv : nvs) {
					String[] kv = nv.split("=");
					String name = kv[0];
					String value = name;
					if (kv.length > 1) {
						value = kv[1];
					}
					handlerOptions.addOption(name, value);
				}
			}
			if (cmd.hasOption('E')) {
				handlerOptions.setEnv(cmd.getOptionValue('E'));
			} else {
				String env = System.getenv("RACK_ENV");
				if (!StringUtils.isBlank(env)) {
					handlerOptions.setEnv(env);
				}
			}
			for (String s : cmd.getArgs()) {
				System.out.println(s);
			}
		} catch (ParseException e) {
			help(options);
		}
	}

	private void showVersion() {
		String version = StringUtils.join(Env.RACK_VERSION_VALUE, ".");
		System.out.println("Rack " + version);
		System.exit(0);
	}

	private void help(Options options) {
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp("rackup [options] [rack config]", options);
		System.exit(0);
	}

	private Options buildOptions() {
		Options options = new Options();
		options.addOption("s", "server", true, "serve using SERVER (jetty)");
		options.addOption("o", "host", true,
				"listen on HOST (default: 0.0.0.0)");
		options.addOption("p", "port", true, "use PORT (default: 9292)");
		options.addOption("O", "option", true,
				"NAME[=Value], pass VALUE to the server as option NAME.");
		options.addOption("E", "env", true,
				"use ENVIRONMENT for defaults (default: development)");
		options.addOption("h", "help", false, "Show this message");
		options.addOption("v", "version", false, "Show version");

		return options;
	}

	public static void main(String[] args) throws Exception {
		Server server = new Server();
		server.parseOptions(args);
		server.start();
	}

}
