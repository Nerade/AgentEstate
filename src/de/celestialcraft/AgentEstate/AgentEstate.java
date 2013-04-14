package de.celestialcraft.AgentEstate;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Random;
import java.util.logging.Logger;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import ru.tehkode.permissions.PermissionManager;
import ru.tehkode.permissions.bukkit.PermissionsEx;

import com.iCo6.system.Holdings;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import de.celestialcraft.MySQLLib.MySQLLib;

import de.celestialcraft.util.*;

public class AgentEstate extends JavaPlugin {					//Hauptklasse des Plugins
	
	//#########Initialisierungen##################
	
	private final AgentEstateBlockListener BlockListener = new AgentEstateBlockListener(
			this);

	public MySQLLib mysql = null;

	public enum Commands {
		CREATE, CREATERENT, SELL, RELEASE, REPAIR, MULTI, MULTIRENT;
	}

	public HashMap<String, Integer> Price = new HashMap<String, Integer>();
	public HashMap<String, String> Seller = new HashMap<String, String>();
	public HashMap<String, String> Region_ID = new HashMap<String, String>();
	public HashMap<String, Integer> confirm = new HashMap<String, Integer>();
	public HashMap<String, Integer> Price_multi = new HashMap<String, Integer>();
	public HashMap<String, Integer> Period = new HashMap<String, Integer>();
	public HashMap<String, Integer> gs_start = new HashMap<String, Integer>();
	public HashMap<String, Commands> Command = new HashMap<String, Commands>();
	public HashMap<String,Estate> estates = new HashMap<>();

	public WorldGuardPlugin worldguard;
	public PermissionManager permissionManager;
	public final SignUtil sign_util = new de.celestialcraft.util.SignUtil(this);
	public final Util util = new de.celestialcraft.util.Util(this);
	public final FileUtil file_util = new de.celestialcraft.util.FileUtil(this);
	boolean startup = true;

	//#########Ende: Initialisierungen##################
	
	Logger log = Logger.getLogger("Minecraft");

	@Override
	public void onDisable() {			// onDisable-Funktion: Daten werden gespeichert und alle Variablen werden freigegeben
		// TODO Auto-generated method stub

		this.saveConfig();
		file_util.saveCustomConfig();
		Price.clear();
		Region_ID.clear();
		Seller.clear();
		Price = null;
		Region_ID = null;
		Seller = null;
		Price_multi = null;
		gs_start = null;
		mysql = null;
		log = null;
		worldguard = null;
		permissionManager = null;
	}

	@Override
	public void onEnable() {					//onEnable-Funktion: Erzeugen einer Config, Verbinden zur Datenbank, Verbindung zu anderen Plugins
		// TODO Auto-generated method stub

		PluginManager pm = this.getServer().getPluginManager();
		PluginDescriptionFile pdfFile = this.getDescription();

		// final Server server = this.getServer();
		// final agentestate plugin = this;

		// ((DBConnector) this.dbconnector)
		// .ensureTable(
		// "agentestate",
		// "`RegionID` VARCHAR( 20 ) NOT NULL , `Owner` VARCHAR( 20 ) NOT NULL , `Seller` VARCHAR( 20 ) NOT NULL DEFAULT 'Bank', `Price` INT( 10 ) NOT NULL DEFAULT '1000', `Area` INT( 10 ) NOT NULL, `x_koord` INT( 10 ) NOT NULL , `y_koord` INT( 10 ) NOT NULL ,`z_koord` INT( 10 ) NOT NULL , UNIQUE ( `RegionID` )");
		boolean first_run = this.getConfig().getBoolean("Reset_Config", true);
		if (first_run) {
			this.getConfig().set("Kuerzel", "PT");
			this.getConfig().set("Waehrung", "Platin");
			this.getConfig().set("price_per_block", 20);
			this.getConfig().set("inactive_days_allowed", 20);
			this.getConfig().set("Default_Period", 7);
			this.getConfig().set("Global-Account-Name", "Bank");
			this.getConfig().set("Save_Rent", true);
			this.getConfig().set("Save_Buy", true);
			this.getConfig().set("mysql-user", "");
			this.getConfig().set("mysql-pw", "");
			this.getConfig().set("mysql-db", "");
			this.getConfig().set("Reset_Config", false);
			this.saveConfig();
			System.out.println("Config file was created!");
		}

		mysql = new MySQLLib(this.getConfig().getString("mysql-user"), this
				.getConfig().getString("mysql-pw"), this.getConfig().getString(
				"mysql-db"));

		try {
			PreparedStatement ps = mysql
					.openConnection()
					.prepareStatement(
							"CREATE TABLE IF NOT EXISTS `agentestate` (`RegionID` varchar(20) NOT NULL,`Owner` varchar(20),`Seller` varchar(20) DEFAULT 'Bank',`Renter` varchar(20),`Price` int(10) DEFAULT -1,`Rent` int(10) DEFAULT 0, `Period` int(10) UNSIGNED, `Expires` TIMESTAMP NULL, `Area` int(10),`World` varchar(20) NOT NULL,`x_koord` int(10) NOT NULL,`y_koord` int(10) NOT NULL,`z_koord` int(10) NOT NULL, UNIQUE KEY `RegionID` (`RegionID`)) ENGINE=MyISAM;");
			System.out.println("Richte Datenbank ein...");
			ps.executeUpdate();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			mysql.closeConnection();
		}

		setupPermissions();
		worldguard = getWorldGuard();

		pm.registerEvents(BlockListener, this);
		
		log.info(pdfFile.getName() + " version " + pdfFile.getVersion()
				+ " is enabled.");
		this.getServer().getScheduler().scheduleSyncDelayedTask(this, new Runnable() {

			@Override
			public void run() {
				log.info("Es wurden " + clearDB() + " fehlerhafte Einträge repariert");			
			}	
		}, 200L);
		

	}

	private void setupPermissions() {			//Methode zur Verbindung mit dem Rechteverwaltungsplugin PermissionsEx

		if (permissionManager != null) {
			return;
		}

		Plugin permissionsPlugin = this.getServer().getPluginManager()
				.getPlugin("PermissionsEx");
		// Plugin permissionsPlugin =
		// this.getServer().getPluginManager().getPlugin("Permissions");

		if (permissionsPlugin == null) {
			log.info("Permission system not detected, defaulting to OP");
			return;
		}

		permissionManager = PermissionsEx.getPermissionManager();
		log.info("Found and will use plugin "
				+ ((PermissionsEx) permissionsPlugin).getDescription()
						.getFullName());

	}

	private int clearDB() {						//Defekte Einträge in der Datenbank löschen

		int x_koord = 0, y_koord = 0, z_koord = 0, price = 0, rent = 0;
		// Player p_el=null;
		String owner, seller = null;
		String world = null;
		String region_id = null;
		Timestamp expires = null;
		long time_off = 0;
		int block_Id = 0;
		Block block;
		int counter = 0;
		// int area = 0;

		try {
			PreparedStatement ps = mysql.openConnection().prepareStatement(
					"SELECT * FROM agentestate");

			ResultSet rs = ps.executeQuery();

			while (rs.next()) {

				region_id = rs.getString("RegionID");
				price = rs.getInt("Price");
				rent = rs.getInt("Rent");
				expires = rs.getTimestamp("Expires");
				world = rs.getString("World");
				x_koord = rs.getInt("x_koord");
				y_koord = rs.getInt("y_koord");
				z_koord = rs.getInt("z_koord");
				// area = rs.getInt("Area");
				owner = rs.getString("Owner");
				seller = rs.getString("Seller");
				block = this.getServer().getWorld(world)
						.getBlockAt(x_koord, y_koord, z_koord);
				block_Id = block.getTypeId();
				if (!(block_Id == 63) && !(block_Id == 68)) {

					PreparedStatement ps2 = mysql
							.openConnection()
							.prepareStatement(
									"DELETE FROM agentestate WHERE World=? AND x_koord=? AND y_koord=? AND z_koord=?");
					ps2.setString(1, world);
					ps2.setInt(2, x_koord);
					ps2.setInt(3, y_koord);
					ps2.setInt(4, z_koord);
					ps2.execute();
					counter++;

				} else {
					OfflinePlayer p_off = this.getServer().getOfflinePlayer(
							owner);
					time_off = System.currentTimeMillis()
							- p_off.getLastPlayed();
					if (time_off > (this.getConfig().getInt(
							"inactive_days_allowed", 20) * 24 * 3600 * 1000)) {

					}
					// for (OfflinePlayer p_element : p_array) {
					//
					// last_login = p_element.getLastPlayed();
					// if (p_element.getName().equals(owner)) {
					//
					// if ((System.currentTimeMillis() - last_login) > (this
					// .getConfig().getInt(
					// "inactive_days_allowed", 20) * 24 * 3600 * 1000)) {
					// Sign sign = (Sign) block.getState();
					// sell(p_element,sign,area*this.getConfig().getInt("price_per_block",
					// 20));
					// }
					// }
					// }
					Sign sign = (Sign) block.getState();
					if (sign.getLine(1).equals("")
							|| !sign.getLine(1).substring(2).equals(region_id)) {
						if (price == -1 && rent != 0) {
							this.sign_util.updateSign(sign, "", region_id,
									owner, "");
						} else if(rent == 0){
							this.sign_util.updateSign(sign, "Zu Verkaufen:",
									region_id, seller, String.valueOf(price));
						} else {
							this.sign_util.updateSign(sign, "Zu Vermieten:",
									region_id, owner, String.valueOf(rent));
						}
						counter++;
					}
				}
				
				if(expires != null && expires.before(new Timestamp(System.currentTimeMillis()))){
					
					Estate newEstate;
					if(estates.containsKey(region_id)){
						newEstate = estates.get(region_id);
					} else {
						newEstate = new Estate(region_id,this);	
					}
					newEstate.release();
				}
			}

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			mysql.closeConnection();
		}
		// System.out.println("Es wurden " + counter
		// + " fehlerhafte Einträge entfernt!");
		return counter;
	}

	private Location getGs_rand(Player p) {		//Position eines zufälligen Eintrags nach diversen Kriterien ausgeben

		if (this.file_util.getCustomConfig().getInt(p.getName()) < 1) {
			double x = 0, y = 0, z = 0;
			String world = null;
			int i = 0;
			try {
				// System.out.println("Test");
				PreparedStatement ps = mysql
						.openConnection()
						.prepareStatement(
								"SELECT RegionID,world,x_koord,y_koord,z_koord FROM agentestate WHERE Price = -1 AND Rent < ? AND Renter IS NULL ORDER BY Rent DESC");
				double temp = new Holdings(p.getName()).getBalance();
				ps.setInt(1, (int) temp);
				ResultSet rs = ps.executeQuery();

				if (rs.first() == true) {
					while (rs.next()) {
						// System.out.println("Test");

						// rs.next();
						//System.out.println(rs.getString("RegionID"));
						i++;
					}
					// do{
					rs.first();

					Random rand = new Random();
					rand.setSeed(System.currentTimeMillis());
					rs.absolute(rand.nextInt(i)+1);
					// } while(rs.getString("RegionID").contains("lb"));
				} else {
					p.sendMessage(ChatColor.RED
							+ "Es stehen dir keine Grundstücke zur Verfügung!");
					return null;
				}
				world = rs.getString("world");
				x = rs.getInt("x_koord");
				y = rs.getInt("y_koord");
				z = rs.getInt("z_koord");

			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return null;
			} finally {
				mysql.closeConnection();
			}
			World tmp_world = this.getServer().getWorld(world);
			Location signLocation = new Location(tmp_world, x, y, z);
			if(tmp_world.getBlockTypeIdAt((signLocation))== 63){
				return signLocation;
			} else {
				Sign sign = (Sign) tmp_world.getBlockAt(signLocation).getState();
				switch(sign.getRawData()){
				
				case 0x2:
					return new Location(tmp_world, x-1, y, z);
				case 0x3:
					return new Location(tmp_world, x+1, y, z);
				case 0x4:
					return new Location(tmp_world, x, y, z+1);
				case 0x5:
					return new Location(tmp_world, x, y, z-1);
				}
			}
		}
		p.sendMessage(ChatColor.RED + "Du hast doch bereits ein Grundstück!");
		return null;
	}

	private boolean updatePrices(Player p) {	//Methode zur Aktualisierung der Grundstückspreise

		try {
			Connection con = mysql.openConnection();
			PreparedStatement ps = con.prepareStatement(
					"SELECT * FROM agentestate WHERE Seller=? OR Owner=? FOR UPDATE");

			ps.setString(1, this.getConfig().getString("Global-Account-Name"));
			ps.setString(2, this.getConfig().getString("Global-Account-Name"));
			ResultSet rs = ps.executeQuery();
			// HashMap<String, Integer> temp = new HashMap<String, Integer>();
			int block_price = this.getConfig().getInt("price_per_block", 20);
			PreparedStatement ps2;
			int cost;
			while (rs.next()) {
				//System.out.println("Ein weiteres Ergebnis");
				Estate curEstate=new Estate(rs.getString("RegionID"),this);
				
				if(curEstate.isRentable()){
					ps2  = con.prepareStatement(
								"UPDATE agentestate SET Rent=? WHERE RegionID=?");
					ps2.setInt(1, block_price * rs.getInt("Area") / 5);
					ps2.setString(2, rs.getString("RegionID"));
					cost=block_price * rs.getInt("Area") / 5;
				} else {
					ps2 = con.prepareStatement(
								"UPDATE agentestate SET Price=? WHERE RegionID=?");
					ps2.setInt(1, block_price * rs.getInt("Area"));
					ps2.setString(2, rs.getString("RegionID"));
					cost=block_price * rs.getInt("Area");
				}
				Sign sign = (Sign) this.getServer().getWorld(curEstate.getWorld()).getBlockAt(curEstate.getLocation()).getState();
				sign.setLine(3,
						"§a" + String.valueOf(cost)
							+ this.getConfig().getString("Kuerzel"));
				sign.update();
				ps2.execute();
				// temp.put(rs.getString("RegionID"),rs.getInt("Area"));

			}
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		} finally {
			mysql.closeConnection();
			mysql.reload(estates, this);
		}
		p.sendMessage("Preise in der Datenbank aktualisiert!");
		//System.out.println("Preise in der Datenbank aktualisiert!");
		return true;
	}

	public boolean onCommand(CommandSender sender, Command cmd,		//onCommand-Methode: Alle Befehle werden hier abgearbeitet
			String commandLabel, String[] args) {

		if (cmd.getLabel().equalsIgnoreCase("agentestate")
				|| cmd.getLabel().equalsIgnoreCase("gs")) {
			if (sender instanceof Player) {
				if (args.length > 0) {
					// System.out.println("Debug_point");

					if (args[0].equalsIgnoreCase("sell")) {			//sell: Befehl zum Verkauf des Grundstückes
						if (args.length == 2) {
							if (this.permissionManager.has((Player) sender,
									"agentestate.sell")) {
								if (Integer.valueOf(args[1]) >= 0) {
									Price.put(sender.getName(),
											Integer.valueOf(args[1]));
									Seller.put(sender.getName(),
											sender.getName());
									Command.put(sender.getName(), Commands.SELL);
									sender.sendMessage("Klicke auf dein Schild um das Grundstück zu verkaufen!");
									return true;
								}
							}
						}
					}if (args[0].equalsIgnoreCase("release")) {			//release: Befehl zum Freilassen eines gemieteten Grundstückes
						if (args.length == 1) {
							if (this.permissionManager.has((Player) sender,
									"agentestate.sell")) {
									Command.put(sender.getName(), Commands.RELEASE);
									sender.sendMessage("Klicke auf dein Schild um das Grundstück freizugeben!");
									return true;
								}
							}
						} else if (args[0].equalsIgnoreCase("start")) {	//start: Befehl zur Zuweisung eines anfänglichen Grundstückes

						if (this.permissionManager.has((Player) sender,
								"agentestate.start")) {
							Location loc = getGs_rand((Player) sender);
							if (loc != null) {
								((Player) sender).teleport(loc);
								sender.sendMessage("Sollte dir das Grundstück nicht gefallen, gib /gs start erneut ein!");
							} else {
//								sender.sendMessage("Es ist ein Fehler aufgetreten. Versuche es bitte erneut!");
							}
							return true;

						}

					} else if (args[0].equalsIgnoreCase("restart")) {	//restart: Zurücksetzen der internen Liste für den start-Befehl

						if (this.permissionManager.has((Player) sender,
								"agentestate.start")) {

							this.gs_start.remove(sender.getName());
							sender.sendMessage("Du erhälst nun eine neue Liste...");
							Location loc = getGs_rand((Player) sender);
							((Player) sender).teleport(loc);
							sender.sendMessage("Sollte dir das Grundstück nicht gefallen, gib /gs start erneut ein!");
							return true;

						}

					}
					// ##### ADMIN Befehle!#############

					else if (args[0].equalsIgnoreCase("create")) {		//create: Definiert ein neues Grundstück
						if (args.length == 3) {
							if (this.permissionManager.has((Player) sender,
									"agentestate.create")) {
								if (Integer.valueOf(args[2]) >= 0) {

									Price.put(sender.getName(),
											Integer.valueOf(args[2]));

									Region_ID.put(sender.getName(), args[1]);
									Command.put(sender.getName(),
											Commands.CREATE);
									sender.sendMessage("Klicke auf ein Schild um es zu koppeln!");
									return true;
								}
							}
						}
					} else if (args[0].equalsIgnoreCase("createrent")) {	//createrent: Definiert neues Grundstück zur Vermietung
						if (args.length == 3||args.length == 4) {
							if (this.permissionManager.has((Player) sender,
									"agentestate.create")) {
								if (Integer.valueOf(args[2]) >= 0) {

									Price.put(sender.getName(),
											Integer.valueOf(args[2]));

									Region_ID.put(sender.getName(), args[1]);
									if(args.length == 4){
										Period.put(sender.getName(), Integer.valueOf(args[3]));
									}
									Command.put(sender.getName(),
											Commands.CREATERENT);
									sender.sendMessage("Klicke auf ein Schild um es zu koppeln!");
									return true;
								}
							}
						}	
					} else if (args[0].equalsIgnoreCase("repair")) {		//repair: Repariert ein zerstörtes Schild vor einem Grundstück

						if (args.length == 3) {
							if (this.permissionManager.has((Player) sender,
									"agentestate.create")) {

								Seller.put(sender.getName(), args[2]);
								Region_ID.put(sender.getName(), args[1]);
								Command.put(sender.getName(), Commands.REPAIR);
								sender.sendMessage("Klicke auf ein Schild um es neu zu koppeln!");
								return true;

							}
						}
					} else if (args[0].equalsIgnoreCase("multimode")		//multimode: Ermöglicht mehrere Grundstücke am Stück zu defninieren
							|| args[0].equalsIgnoreCase("mm")) {
						if (args.length == 2) {

							if (this.permissionManager.has((Player) sender,
									"agentestate.create")) {

								if (args[1].equals("off")) {

									this.Price_multi.remove(sender.getName());
									this.Command.remove(sender.getName());
									sender.sendMessage("Multimode deaktiviert!");
									return true;
								} else {

									this.Price_multi.put(sender.getName(),
											Integer.valueOf(args[1]));
									sender.sendMessage("Multimode mit Preis "
											+ Integer.valueOf(args[1])
											+ " aktiviert!\n Zum Deaktivieren /gs multimode off eintippen.");
									Command.put(sender.getName(),
											Commands.MULTI);
									return true;
								}
							}
						}
					} else if (args[0].equalsIgnoreCase("multirent")		//multimode: Ermöglicht mehrere Grundstücke am Stück zu defninieren
							|| args[0].equalsIgnoreCase("mr")) {
						if (args.length == 2) {

							if (this.permissionManager.has((Player) sender,
									"agentestate.create")) {

								if (args[1].equals("off")) {

									this.Price_multi.remove(sender.getName());
									this.Command.remove(sender.getName());
									sender.sendMessage("Multirent deaktiviert!");
									return true;
								} else {

									this.Price_multi.put(sender.getName(),
											Integer.valueOf(args[1]));
									sender.sendMessage("Multirent mit Miete "
											+ Integer.valueOf(args[1])
											+ " aktiviert!\n Zum Deaktivieren /gs multirent off eintippen.");
									Command.put(sender.getName(),
											Commands.MULTIRENT);
									return true;
								}
							}
						}
					} else if (args[0].equalsIgnoreCase("clear")) {			//clear: Reinigt die Datenbank
						if (this.permissionManager.has((Player) sender,
								"agentestate.admin")) {

							sender.sendMessage("Es wurden " + clearDB()
									+ " fehlerhafte Einträge repariert!");
							return true;
						}
					} else if (args[0].equalsIgnoreCase("update")) {		//update: Berechnet Grundstückspreise neu
						if (this.permissionManager.has((Player) sender,
								"agentestate.admin")) {

							this.updatePrices((Player) sender);
							return true;
						}
					} else if (args[0].equalsIgnoreCase("reload")) {		//reload: Übernimmt neue Einstellungen aus der Config-File
						if (this.permissionManager.has((Player) sender,
								"agentestate.admin")) {

							this.reloadConfig();
							this.file_util.reloadCustomConfig();
							return true;
						}
					} else if (args[0].equalsIgnoreCase("list")) {			//list: Suche von Grundstücken nach einstellbaren Kriterien
						if (this.permissionManager.has((Player) sender,
								"agentestate.list")) {
							java.util.Vector<String> results = list_gs(args);
							if (!results.isEmpty()) {
								if (results.capacity() <= 10) {
									sender.sendMessage(ChatColor.GREEN
											+ "Results of the search:");
									for (String gs : results) {
										sender.sendMessage(gs);
									}
								} else {
									sender.sendMessage(ChatColor.RED
											+ "Too much results! Please use better filter!");
								}
							} else {
								sender.sendMessage(ChatColor.RED
										+ "No results found!");

							}
							return true;

						}

					}
				}
				} else if (args[0].equalsIgnoreCase("reload")) {

					this.reloadConfig();
					this.file_util.reloadCustomConfig();
					return true;

				}
			}
		
		help((Player) sender);
		return true;
	}

	private java.util.Vector<String> list_gs(String[] args) {		//Hilfs-Methode zur Suche in der Datenbank

		String par[] = { "%", "%", "%", "%" };
		for (int i = 0; i < args.length - 1; i++) {
			if (!args[i + 1].equals("*")) {
				par[i] = args[i + 1];
			}
		}

		// TODO Auto-generated method stub
		return getList(par[0], par[1], par[2], par[3]);
	}
	
	private java.util.Vector<String> getList(String region, String owner,	//Haupt-Methode zur Suche in der Datenbank
			String seller, String price) {

		java.util.Vector<String> results = new java.util.Vector<String>();
		try {
			PreparedStatement ps = mysql
					.openConnection()
					.prepareStatement(
							"SELECT * FROM agentestate WHERE Owner LIKE ? AND Seller LIKE ? AND Price LIKE ? AND RegionID LIKE ?");
			ps.setString(1, owner);
			ps.setString(2, seller);
			ps.setString(3, price);
			ps.setString(4, region);
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {

				results.add(rs.getString("RegionID") + "//"
						+ rs.getString("Owner") + "//" + rs.getString("Seller")
						+ "//" + rs.getString("Price"));

			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return results;
	}

	private WorldGuardPlugin getWorldGuard() {		//Schnittstelle zum WorldGuard-Plugin

		Plugin worldGuard = getServer().getPluginManager().getPlugin(
				"WorldGuard");

		// WorldGuard may not be loaded
		if (worldGuard == null || !(worldGuard instanceof WorldGuardPlugin)) {
			return null; // Maybe you want throw an exception instead
		}

		return (WorldGuardPlugin) worldGuard;

	}

	private void help(Player p) {					//Routine zur Erzeugung einer formattierten Hilfe-Ausgabe

		p.sendMessage("Command Reference for AgentEstate:");
		if (this.permissionManager.has(p, "agentestate.admin")) {

			p.sendMessage(ChatColor.GREEN + "/gs clear " + ChatColor.WHITE
					+ " // Ueberprueft und reinigt Datenbank/Gibt Grundstuecke frei(ADMIN)");
			p.sendMessage(ChatColor.GREEN + "/gs reload " + ChatColor.WHITE
					+ " // Laedt die Config neu(ADMIN)");
			p.sendMessage(ChatColor.GREEN + "/gs update " + ChatColor.WHITE
					+ " // Updatet die Preise (ADMIN)");

		}
		if (this.permissionManager.has(p, "agentestate.create")) {

			p.sendMessage(ChatColor.GREEN
					+ "/gs multimode <preis> "
					+ ChatColor.WHITE
					+ " // Aktiviert den Multimode(ADMIN) (Preis -1 = autopreis)");
			p.sendMessage(ChatColor.GREEN + "/gs create <region_id> <preis> "
					+ ChatColor.WHITE
					+ " // Erstellt einen neuen Makler (ADMIN)");
			p.sendMessage(ChatColor.GREEN + "/gs repair <region_id> <owner> "
					+ ChatColor.WHITE
					+ " // Ersetzt ein zerstoertes Schild (ADMIN)");

		}
		if (this.permissionManager.has(p, "agentestate.createrent")){
			p.sendMessage(ChatColor.GREEN + "/gs createrent <region_id> <rent> <period>(optional)"
					+ ChatColor.WHITE
					+ " // Erstellt ein mietbares Grundstueck (ADMIN)");
			
		}
		if (this.permissionManager.has(p, "agentestate.list")) {

			p.sendMessage(ChatColor.GREEN
					+ "/gs list <region> <owner> <seller> <price> "
					+ ChatColor.WHITE
					+ " // Sucht nach GS. Soll nach allem gesucht werden '*' verwenden");

		}

		p.sendMessage(ChatColor.GREEN + "/gs sell <preis> " + ChatColor.WHITE
				+ " // Setzt dein Grundstueck zum Verkauf");
		p.sendMessage(ChatColor.GREEN + "/gs start " + ChatColor.WHITE
				+ " // Bringt dich zu einem freien Grundstueck");
		p.sendMessage(ChatColor.GREEN + "/gs restart " + ChatColor.WHITE
				+ " // Resettet die interne Liste an freien Grundstuecken");

	}

}
