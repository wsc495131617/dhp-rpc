package org.dhp.common.utils;

import java.lang.management.ManagementFactory;
import java.util.List;

public class ArgsUtils {
	
	public static JVMArgs createJVMArgs() {
		return new JVMArgs(ManagementFactory.getRuntimeMXBean().getInputArguments());
	}
	
	public static class JVMArgs {
		protected List<String> args;
		public JVMArgs(List<String> args) {
			this.args = args;
		}
		
		public String getDValue(String name) {
			for(String arg : args) {
				if(arg.startsWith("-D")) {
					int index = arg.indexOf("=");
					if(index > 2){
						String key = arg.substring(2,index);
						if(key.equals(name)) {
							return arg.substring(index+1);
						}
					}
				}
			}
			return null;
		}
		
		public String getXValue(String name) {
			return null;
		}
		
		public String getXXValue(String name) {
			return null;
		}
		
		public String getVerbose() {
			for(String arg : args) {
				if(arg.startsWith("-verbose")) {
					int index = arg.indexOf(":");
					return arg.substring(index+1);
				}
			}
			return null;
		}
	}
}
