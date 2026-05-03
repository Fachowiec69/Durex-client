import discord
from discord import app_commands
from discord.ui import Select, View, Button
import os
from datetime import datetime

intents = discord.Intents.default()
intents.message_content = True
intents.guilds = True
intents.members = True

client = discord.Client(intents=intents)
tree = app_commands.CommandTree(client)

# Kategorie ticketów
CATEGORIES = {
    "zakup": {"emoji": "🍃", "name": "Zakup", "description": "Kliknij, aby otworzyć ticket w tej kategorii."},
    "odbiór_nagrody": {"emoji": "🎁", "name": "Odbiór nagrody", "description": "Kliknij, aby otworzyć ticket w tej kategorii."},
    "sprzedaż": {"emoji": "🏛️", "name": "Sprzedaż", "description": "Kliknij, aby otworzyć ticket w tej kategorii."},
    "wymiana": {"emoji": "🔄", "name": "Wymiana", "description": "Kliknij, aby otworzyć ticket w tej kategorii."},
    "inne": {"emoji": "❓", "name": "Inne", "description": "Kliknij, aby otworzyć ticket w tej kategorii."}
}

class TicketCategorySelect(Select):
    def __init__(self):
        options = [
            discord.SelectOption(
                label=cat["name"],
                emoji=cat["emoji"],
                value=key,
                description=cat["description"]
            )
            for key, cat in CATEGORIES.items()
        ]
        super().__init__(
            placeholder="Wybierz odpowiednią kategorię, aby stworzyć ticketa.",
            options=options,
            custom_id="ticket_category_select"
        )
    
    async def callback(self, interaction: discord.Interaction):
        category_key = self.values[0]
        category = CATEGORIES[category_key]
        
        guild = interaction.guild
        user = interaction.user
        
        # Sprawdź czy user już ma otwarty ticket
        existing_channel = discord.utils.get(guild.text_channels, topic=f"Ticket użytkownika {user.id}")
        if existing_channel:
            await interaction.response.send_message(
                f"❌ Masz już otwarty ticket: {existing_channel.mention}",
                ephemeral=True
            )
            return
        
        # Znajdź kategorię Discord dla ticketów (opcjonalne)
        ticket_category = discord.utils.get(guild.categories, name="TICKETS")
        
        # Utwórz prywatny kanał
        overwrites = {
            guild.default_role: discord.PermissionOverwrite(read_messages=False),
            user: discord.PermissionOverwrite(read_messages=True, send_messages=True),
            guild.me: discord.PermissionOverwrite(read_messages=True, send_messages=True)
        }
        
        channel_name = f"ticket-{user.name}".lower().replace(" ", "-")
        ticket_channel = await guild.create_text_channel(
            name=channel_name,
            category=ticket_category,
            overwrites=overwrites,
            topic=f"Ticket użytkownika {user.id}"
        )
        
        # Embed w tickecie
        embed = discord.Embed(
            title=f"{category['emoji']} {category['name']} - Ticket",
            description=f"Witaj {user.mention}!\n\nDziękujemy za utworzenie ticketa w kategorii **{category['name']}**.\nAdministracja wkrótce się z Tobą skontaktuje.",
            color=discord.Color.blue(),
            timestamp=datetime.utcnow()
        )
        embed.set_footer(text=f"Ticket utworzony przez {user.name}", icon_url=user.display_avatar.url)
        
        # Przycisk zamknięcia
        close_button = Button(label="Zamknij ticket", style=discord.ButtonStyle.danger, emoji="🔒")
        
        async def close_callback(button_interaction: discord.Interaction):
            await button_interaction.response.send_message("🔒 Zamykam ticket...", ephemeral=True)
            await ticket_channel.delete(reason=f"Ticket zamknięty przez {button_interaction.user}")
        
        close_button.callback = close_callback
        view = View(timeout=None)
        view.add_item(close_button)
        
        await ticket_channel.send(embed=embed, view=view)
        await interaction.response.send_message(
            f"✅ Ticket utworzony: {ticket_channel.mention}",
            ephemeral=True
        )

class TicketView(View):
    def __init__(self):
        super().__init__(timeout=None)
        self.add_item(TicketCategorySelect())

@client.event
async def on_ready():
    await tree.sync()
    print(f'✅ Bot zalogowany jako {client.user}')

@tree.command(name="ticket-setup", description="Wyślij wiadomość z panelem ticketów")
@app_commands.checks.has_permissions(administrator=True)
async def ticket_setup(interaction: discord.Interaction):
    embed = discord.Embed(
        title="🎫 || Otwórz ticket!",
        description="To jest początek kanału #🎫 || otwórz ticket.",
        color=discord.Color.dark_gray()
    )
    
    view = TicketView()
    await interaction.channel.send(embed=embed, view=view)
    await interaction.response.send_message("✅ Panel ticketów wysłany!", ephemeral=True)

# Uruchom bota
TOKEN = os.getenv("DISCORD_BOT_TOKEN")
if not TOKEN:
    print("❌ Brak tokenu! Ustaw zmienną środowiskową DISCORD_BOT_TOKEN")
else:
    client.run(TOKEN)
