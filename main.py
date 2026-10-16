from kivy.app import App
from kivy.uix.boxlayout import BoxLayout
from kivy.uix.button import Button
from kivy.uix.label import Label
from kivy.uix.scrollview import ScrollView
from kivy.uix.gridlayout import GridLayout
from kivy.core.window import Window
from kivy.uix.screenmanager import ScreenManager, Screen
import sqlite3
import os

Window.softinput_mode = "below_target"

# Find DB - it will be in same folder as main.py after build
DB_PATH = "enduring_word_full.db"
if not os.path.exists(DB_PATH):
    # Try alternative path for Android
    DB_PATH = "/data/data/org.enduringword/files/app/enduring_word_full.db"
if not os.path.exists(DB_PATH):
    DB_PATH = os.path.join(os.getcwd(), "enduring_word_full.db")

BOOKS = ["Genesis","Exodus","Leviticus","Numbers","Deuteronomy","Joshua","Judges","Ruth","1 Samuel","2 Samuel","1 Kings","2 Kings","1 Chronicles","2 Chronicles","Ezra","Nehemiah","Esther","Job","Psalms","Proverbs","Ecclesiastes","Song of Solomon","Isaiah","Jeremiah","Lamentations","Ezekiel","Daniel","Hosea","Joel","Amos","Obadiah","Jonah","Micah","Nahum","Habakkuk","Zephaniah","Haggai","Zechariah","Malachi","Matthew","Mark","Luke","John","Acts","Romans","1 Corinthians","2 Corinthians","Galatians","Ephesians","Philippians","Colossians","1 Thessalonians","2 Thessalonians","1 Timothy","2 Timothy","Titus","Philemon","Hebrews","James","1 Peter","2 Peter","1 John","2 John","3 John","Jude","Revelation"]

class BookScreen(Screen):
    def on_enter(self):
        self.clear_widgets()
        layout = BoxLayout(orientation='vertical')
        layout.add_widget(Label(text="Enduring Word - Offline", size_hint_y=None, height=60, font_size='22sp', bold=True))
        
        scroll = ScrollView()
        grid = GridLayout(cols=1, spacing=5, padding=10, size_hint_y=None)
        grid.bind(minimum_height=grid.setter('height'))
        
        for idx, book in enumerate(BOOKS):
            btn = Button(text=book, size_hint_y=None, height=60, font_size='20sp')
            btn.bind(on_press=lambda x, b=book, i=idx+1: self.open_book(b, i))
            grid.add_widget(btn)
        
        scroll.add_widget(grid)
        layout.add_widget(scroll)
        self.add_widget(layout)

    def open_book(self, book_name, book_num):
        app = App.get_running_app()
        app.current_book = book_name
        app.current_book_num = book_num
        app.root.current = 'chapter'

class ChapterScreen(Screen):
    def on_enter(self):
        self.clear_widgets()
        app = App.get_running_app()
        layout = BoxLayout(orientation='vertical')
        
        top = BoxLayout(size_hint_y=None, height=60)
        back = Button(text="< Books", size_hint_x=None, width=100, font_size='18sp')
        back.bind(on_press=lambda x: setattr(app.root, 'current', 'book'))
        top.add_widget(back)
        top.add_widget(Label(text=app.current_book, font_size='22sp', bold=True))
        layout.add_widget(top)
        
        scroll = ScrollView()
        grid = GridLayout(cols=5, spacing=5, padding=10, size_hint_y=None)
        grid.bind(minimum_height=grid.setter('height'))
        
        # Try to get max chapter from DB, else default 50
        max_ch = 50
        try:
            conn = sqlite3.connect(DB_PATH)
            cur = conn.cursor()
            cur.execute("SELECT MAX(chapter) FROM commentary WHERE book_number=?", (app.current_book_num,))
            r = cur.fetchone()
            if r and r[0]: max_ch = r[0]
            conn.close()
        except: pass
        
        if max_ch > 150: max_ch = 150
        
        for ch in range(1, max_ch+1):
            btn = Button(text=str(ch), size_hint_y=None, height=60, font_size='20sp')
            btn.bind(on_press=lambda x, c=ch: self.open_chapter(c))
            grid.add_widget(btn)
        
        scroll.add_widget(grid)
        layout.add_widget(scroll)
        self.add_widget(layout)
    
    def open_chapter(self, ch):
        app = App.get_running_app()
        app.current_chapter = ch
        app.root.current = 'commentary'

class CommentaryScreen(Screen):
    def on_enter(self):
        self.clear_widgets()
        app = App.get_running_app()
        layout = BoxLayout(orientation='vertical')
        
        top = BoxLayout(size_hint_y=None, height=60)
        back = Button(text="< Chapters", size_hint_x=None, width=120, font_size='18sp')
        back.bind(on_press=lambda x: setattr(app.root, 'current', 'chapter'))
        top.add_widget(back)
        top.add_widget(Label(text=f"{app.current_book} {app.current_chapter}", font_size='20sp', bold=True))
        layout.add_widget(top)
        
        scroll = ScrollView()
        content_box = BoxLayout(orientation='vertical', size_hint_y=None, padding=15, spacing=15)
        content_box.bind(minimum_height=content_box.setter('height'))
        
        try:
            conn = sqlite3.connect(DB_PATH)
            cur = conn.cursor()
            # Try multiple possible table schemas
            commentary_text = ""
            try:
                cur.execute("SELECT commentary FROM commentary WHERE book_number=? AND chapter=? ORDER BY verse", (app.current_book_num, app.current_chapter))
                rows = cur.fetchall()
                for row in rows:
                    commentary_text += str(row[0]) + "\n\n"
            except:
                try:
                    cur.execute("SELECT text FROM commentary WHERE book=? AND chapter=?", (app.current_book, app.current_chapter))
                    rows = cur.fetchall()
                    for row in rows:
                        commentary_text += str(row[0]) + "\n\n"
                except Exception as e:
                    commentary_text = f"DB Schema check:\n{e}\n\nTrying to list tables...\n"
                    cur.execute("SELECT name FROM sqlite_master WHERE type='table'")
                    tables = cur.fetchall()
                    commentary_text += str(tables)
            
            conn.close()
            
            if not commentary_text.strip():
                commentary_text = "No commentary found for this chapter in DB. DB path: " + DB_PATH
            
            lbl = Label(text=commentary_text, font_size='22sp', text_size=(Window.width - 30, None), size_hint_y=None, halign='left', valign='top')
            lbl.bind(texture_size=lambda instance, value: setattr(instance, 'height', value[1]))
            content_box.add_widget(lbl)
            
        except Exception as e:
            err_lbl = Label(text=f"Error loading DB:\n{e}\n\nPath: {DB_PATH}\nExists: {os.path.exists(DB_PATH)}", font_size='18sp', text_size=(Window.width - 30, None), size_hint_y=None)
            err_lbl.bind(texture_size=lambda instance, value: setattr(instance, 'height', value[1]))
            content_box.add_widget(err_lbl)
        
        scroll.add_widget(content_box)
        layout.add_widget(scroll)
        self.add_widget(layout)

class EnduringWordApp(App):
    def build(self):
        self.current_book = "Genesis"
        self.current_book_num = 1
        self.current_chapter = 1
        sm = ScreenManager()
        sm.add_widget(BookScreen(name='book'))
        sm.add_widget(ChapterScreen(name='chapter'))
        sm.add_widget(CommentaryScreen(name='commentary'))
        return sm

EnduringWordApp().run()