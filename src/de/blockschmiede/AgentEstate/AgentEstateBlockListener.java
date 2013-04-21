package de.celestialcraft.AgentEstate;

import java.text.SimpleDateFormat;

import org.bukkit.ChatColor;

import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;

import com.sk89q.worldguard.protection.regions.ProtectedRegion;

import de.celestialcraft.AgentEstate.AgentEstate.Commands;

public class AgentEstateBlockListener implements Listener { // Blocklistener des
															// Plugins

	public AgentEstate plugin;

	public AgentEstateBlockListener(AgentEstate instance) {
		plugin = instance;
	}

	@EventHandler
	public void onBlockBreak(BlockBreakEvent block) { // Sobald ein Block (in
														// diesem Fall nur
														// Schild) zerstört wird

		if ((block.getBlock().getTypeId() == 63)
				|| (block.getBlock().getTypeId() == 68)) {
			Sign sign = (Sign) block.getBlock().getState();
			if (plugin.sign_util.isMarkler(sign)) {
				Player p = block.getPlayer();
				if (!plugin.permissionManager.has(p, "agentestate.remove")) {
					block.setCancelled(true);
					sign.update();
					p.sendMessage(ChatColor.RED
							+ "Du hast nicht die Rechte, um Makler zu entfernen!");
				} else {
					if (p.getGameMode().toString().equals("SURVIVAL")
							|| p.isSneaking()) {
						Estate newEstate;
						
						if(plugin.estates.containsKey(sign.getLine(1).substring(2))){
							newEstate = plugin.estates.get(sign.getLine(1).substring(2));
						} else {
							newEstate = new Estate(sign.getLine(1).substring(2),plugin);	
						}
						if (newEstate.remove()) {
							p.sendMessage("Makler erfolgreich entfernt!");
						} else {
							p.sendMessage("Beim Entfernen ist ein Fehler aufgetreten!");
						}
					} else {
						block.setCancelled(true);
						p.sendMessage("Solltest du das Schild wirklich entfernen wollen nutze den Survival-Mode.");

					}

				}
			} else if (block.getPlayer().getItemInHand().getTypeId() == 287) {

				if (plugin.permissionManager.has(block.getPlayer(),
						"agentestate.create")) {
					block.setCancelled(true);
					plugin.getServer()
							.getPluginManager()
							.callEvent(
									new BlockDamageEvent(block.getPlayer(),
											block.getBlock(), block.getPlayer()
													.getItemInHand(), false));

				}

			}

		}

	}

	@EventHandler
	public void onBlockDamage(BlockDamageEvent block) { // Sobald ein Block (in
														// diesem Fall nur
														// Schild) beschädigt
														// wird
		Player p = block.getPlayer();

		// ################ SCHILD ANGEKLICKT ###################

		if ((block.getBlock().getTypeId() == 63)
				|| (block.getBlock().getTypeId() == 68)) {

			// System.out.println(plugin.isRegionNear((Sign)
			// block.getBlock().getState()));
			// System.out.println("Debug_point1");
			if (plugin.Command.get(p.getName()) != null) {
				if (plugin.Command.get(p.getName()).equals(Commands.CREATE)) {

					// System.out.println("Debug_point2");
					if (plugin.permissionManager.has(p, "agentestate.create")) {

						// System.out.println("Debug_point3");
						if (p.getItemInHand().getTypeId() == 287) {

							block.setCancelled(true);
							Estate newEstate = new Estate(
									plugin.Region_ID.get(p.getName()), plugin);
							Sign sign = (Sign) block.getBlock().getState();
							newEstate.setOwner("");
							newEstate.setSeller("Bank");
							newEstate.setPrice(plugin.Price.get(p.getName()));
							newEstate.create(sign);

						}

						plugin.Price.remove(p.getName());
						plugin.Seller.remove(p.getName());
						plugin.Region_ID.remove(p.getName());

					}
				} else if (plugin.Command.get(p.getName()).equals(
						Commands.CREATERENT)) {

					// System.out.println("Debug_point2");
					if (plugin.permissionManager.has(p,
							"agentestate.createrent")) {

						// System.out.println("Debug_point3");
						if (p.getItemInHand().getTypeId() == 287) {

							block.setCancelled(true);
							Estate newEstate = new Estate(
									plugin.Region_ID.get(p.getName()), plugin);
							Sign sign = (Sign) block.getBlock().getState();
							newEstate.setOwner(plugin.getConfig()
									.getString("Bank", "Bank"));
							newEstate.setSeller(null);
							newEstate.setPrice(-1);
							if (plugin.Period.get(p.getName()) != null) {
								newEstate.setPeriod(plugin.Period.get(p
										.getName()));
								plugin.Period.remove(p.getName());
							} else {
								newEstate.setPeriod(plugin.getConfig().getInt(
										"Default_Period", 7));
								// System.out.println("Debug1");
							}
							newEstate.setRent(plugin.Price.get(p.getName()));
							newEstate.createRentable(sign);

						}

						plugin.Price.remove(p.getName());
						plugin.Seller.remove(p.getName());
						plugin.Region_ID.remove(p.getName());

					}
				} else if (plugin.Command.get(p.getName()).equals(
						Commands.REPAIR)) {

					if (plugin.permissionManager.has(p, "agentestate.create")) {

						if (p.getItemInHand().getTypeId() == 287) {
							block.setCancelled(true);
							Estate newEstate = new Estate(
									plugin.Region_ID.get(p.getName()), plugin);
							Sign sign = (Sign) block.getBlock().getState();
							newEstate.setOwner(plugin.Seller.get(p.getName()));
							newEstate.setSeller("");
							newEstate.repair(sign,
									plugin.Seller.get(p.getName()));

						}
					}
					plugin.Price.remove(p.getName());
					plugin.Seller.remove(p.getName());
					plugin.Region_ID.remove(p.getName());

				} else if (plugin.Command.get(p.getName()).equals(
						Commands.MULTI)) {

					if (plugin.permissionManager.has(p, "agentestate.create")) {

						if (p.getItemInHand().getTypeId() == 287) {

							block.setCancelled(true);
							ProtectedRegion auto_region = plugin.sign_util
									.isRegionNear((Sign) block.getBlock()
											.getState());
							if (auto_region != null) {
								Estate newEstate = new Estate(
										auto_region.getId(), plugin);
								if (plugin.Price_multi.get(p.getName()) != -1) {

									newEstate.setOwner("");
									newEstate.setSeller(plugin.getConfig()
											.getString("Bank", "Bank"));
									newEstate.setPrice(plugin.Price_multi.get(p
											.getName()));
									if (newEstate.create((Sign) block
											.getBlock().getState())) {

										p.sendMessage("Makler erfolgreich erstellt!");
									}
									// if (plugin.create(p, (Sign)
									// block.getBlock()
									// .getState(), auto_region.getId(),
									// plugin.Price_multi.get(p.getName()))) {
									// p.sendMessage("Markler erstellt!");
									// }
								} else {
									int area = plugin.util.getArea(auto_region);
									newEstate.setOwner("");
									newEstate.setSeller(plugin.getConfig()
											.getString("Bank", "Bank"));
									newEstate.setPrice(area
											* plugin.getConfig().getInt(
													"price_per_block", 20));
									if (newEstate.create((Sign) block
											.getBlock().getState())) {

										p.sendMessage("Makler erfolgreich erstellt!");
									}
								}
							}
						}

					}

				} else if (plugin.Command.get(p.getName()).equals(
						Commands.MULTIRENT)) {

					if (plugin.permissionManager.has(p,
							"agentestate.createrent")) {

						if (p.getItemInHand().getTypeId() == 287) {

							block.setCancelled(true);
							ProtectedRegion auto_region = plugin.sign_util
									.isRegionNear((Sign) block.getBlock()
											.getState());
							if (auto_region != null) {
								Estate newEstate = new Estate(
										auto_region.getId(), plugin);
								if (plugin.Price_multi.get(p.getName()) != -1) {

									newEstate.setOwner(plugin.getConfig()
											.getString("Bank", "Bank"));
									newEstate.setSeller(null);
									newEstate.setPrice(-1);
									newEstate.setRent(plugin.Price_multi.get(p
											.getName()));
									if (newEstate.createRentable((Sign) block
											.getBlock().getState())) {

										p.sendMessage("Makler erfolgreich erstellt!");
									}
									// if (plugin.create(p, (Sign)
									// block.getBlock()
									// .getState(), auto_region.getId(),
									// plugin.Price_multi.get(p.getName()))) {
									// p.sendMessage("Markler erstellt!");
									// }
								} else {
									int area = plugin.util.getArea(auto_region);
									newEstate.setOwner(plugin.getConfig()
											.getString("Bank", "Bank"));
									newEstate.setSeller(null);
									newEstate.setPrice(-1);
									newEstate.setRent(area
											* plugin.getConfig().getInt(
													"price_per_block", 20) / 5);
									if (newEstate.createRentable((Sign) block
											.getBlock().getState())) {

										p.sendMessage("Makler erfolgreich erstellt!");
									}
								}
							}
						}

					}

				} else if (plugin.Command.get(p.getName())
						.equals(Commands.SELL)) {

					if (plugin.permissionManager.has(p, "agentestate.sell")) {
						// System.out.println("Debug_point_1");
						if (!plugin.sign_util.isMarkler((Sign) block.getBlock()
								.getState())) {

							p.sendMessage("Das Schild ist kein Makler!");
							return;
						}
						Estate newEstate = new Estate((Sign) block.getBlock()
								.getState(), plugin);
						// System.out.println(p.getName());
						// System.out.println(newEstate.getOwner());
						if (!newEstate.getOwner().equals(p.getName())) {
							p.sendMessage("Das Grundstück gehört dir nicht!");
							plugin.Command.remove(p.getName());
							return;
						}
						newEstate.setPrice(plugin.Price.get(p.getName()));
						if (newEstate.sell()) {
							p.sendMessage("Das Grundstück steht nun zum Verkauf!");
						}
						plugin.Price.remove(p.getName());
						plugin.Region_ID.remove(p.getName());

						// plugin.sell(p, (Sign) block.getBlock().getState(),
						// plugin.Price.get(p.getName()));

					}

					// ################# KAUFEN ##################
				}

			} else if (plugin.permissionManager.has(p, "agentestate.buy")) {
				Sign sign = (Sign) block.getBlock().getState();

				if (plugin.sign_util.isMarkler(sign)) {

					if (!sign.getLine(0).equals("")) { // Estate is not sold

						int temp = (int) (p.getLocation().getX() * 4);
						temp += p.getLocation().getZ();

						Estate curEstate = new Estate(sign, plugin);
						if (sign.getLine(0).equals("Vermietet!")) { // Estate is
																	// rented
							if (sign.getLine(2).equals(p.getName())) { // Renter
																		// clicked
								if ((plugin.confirm.get(p.getName()) == null)
										|| (plugin.confirm.get(p.getName()) != temp)) {
									p.sendMessage("Du hast bezahlt bis zum "
											+ ChatColor.GREEN
											+ new SimpleDateFormat(
													"dd/MM/yyyy HH:mm")
													.format(curEstate
															.getExpires()));
									p.sendMessage("Klicke ein weiteres mal zum Verlängern um "
											+ ChatColor.GREEN
											+ curEstate.getPeriod()
											+ " Tage"
											+ ChatColor.WHITE + "...");
									plugin.confirm.put(p.getName(), temp);

								} else { // Renter clicked 2nd time
									if (curEstate.rent(p)) {
										p.sendMessage("Du hast deine Miete erfolgreich verlängert!");
									}
								}
							} else { // Non-Renter clicked

								p.sendMessage("Das Grundstück ist bereits vermietet!");
								p.sendMessage("Läuft ab am "
										+ ChatColor.GREEN
										+ new SimpleDateFormat(
												"dd/MM/yyyy HH:mm")
												.format(curEstate.getExpires()));
							}
						} else { // Estate is available

							if ((plugin.confirm.get(p.getName()) == null)
									|| (plugin.confirm.get(p.getName()) != temp)) {
								if (curEstate.isRentable()) {
									p.sendMessage("Klicke ein weiteres mal zur Bestätigung um es für "
											+ ChatColor.GREEN
											+ curEstate.getPeriod()
											+ " Tage "
											+ ChatColor.WHITE + "zu mieten...");
								} else {
									p.sendMessage("Klicke ein weiteres mal zur Bestätigung des Kaufes...");
								}
								plugin.confirm.put(p.getName(), temp);
							} else {
								// System.out.println("Debug_point_2");

								if (curEstate.isRentable()) {
									if (curEstate.rent(p)) {
										p.sendMessage("Du hast das Grundstück nun gemietet!");
									}
								} else {
									if (curEstate.buy(p)) {
										p.sendMessage("Das Grundstück gehört nun dir!");
									}
								}
								plugin.confirm.remove(p.getName());

							}
						}
					} else {
						if (sign.getLine(2).equals(block.getPlayer().getName())) {
							p.sendMessage("Um dein Grundstück zu verkaufen tippe /gs sell <Preis> ein!");
						} else {
							p.sendMessage("Das Grundstück steht nicht zum Verkauf!");
						}
					}
				}
			} else {
				p.sendMessage(ChatColor.RED
						+ "Du hast nicht die nötigen Rechte um das zu tun!");
			}
			if (plugin.Command.get(p.getName()) != Commands.MULTI && plugin.Command.get(p.getName()) != Commands.MULTIRENT) {
				plugin.Command.remove(p.getName());
			}
		}
	}
}
