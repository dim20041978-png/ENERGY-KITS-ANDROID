from pathlib import Path
from subprocess import run
import re
import time
from xml.etree import ElementTree as ET

PACKAGE = 'ua.energykits.app'
ACTIVITY = PACKAGE + '/.MainActivity'
EVIDENCE = Path('device-evidence')
EVIDENCE.mkdir(exist_ok=True)
UI = '/sdcard/energy-verification.xml'
INVERTER = 'Deye SUN-5K-SG03LP1-EU'
BATTERY = 'Dyness PowerBrick Plus'
APP = Path('app/build/outputs/apk/debug/app-debug.apk')
width, height = 1080, 1920

def adb(*args, check=True):
    return run(['adb', *args], check=check, capture_output=True, text=True, timeout=30).stdout.strip()

def dump():
    adb('shell', 'uiautomator', 'dump', UI)
    result = adb('shell', 'cat', UI)
    (EVIDENCE / 'last-window.xml').write_text(result)
    return list(ET.fromstring(result).iter('node'))

def text(node):
    return (node.get('text', '') + ' ' + node.get('content-desc', '')).strip()

def tap(node):
    x1,y1,x2,y2 = (int(n) for n in re.findall(r'\d+', node.get('bounds')))
    adb('shell', 'input', 'tap', str((x1+x2)//2), str((y1+y2)//2))

def search(predicate, seconds=15):
    end = time.monotonic() + seconds
    while time.monotonic() < end:
        candidate = next((n for n in dump() if predicate(n)), None)
        if candidate is not None:
            return candidate
        time.sleep(.4)
    raise AssertionError('Element not found. See last-window.xml.')

def scroll(direction='down'):
    x = str(width//2)
    y1,y2 = (height*3//4,height*2//5) if direction=='down' else (height*2//5,height*3//4)
    adb('shell','input','swipe',x,str(y1),x,str(y2),'500')

def tap_visible(label):
    expected=label.casefold()
    tap(search(lambda n: text(n).casefold()==expected))

def look(label, max_swipes=22):
    expected=label.casefold()
    for _ in range(max_swipes + 1):
        node=next((n for n in dump() if text(n).casefold()==expected),None)
        if node is not None:
            return node
        scroll()
    raise AssertionError(f'Element not found while scrolling: {label}')

def select_row(label, action):
    for _ in range(26):
        nodes=dump()
        for index,node in enumerate(nodes):
            if text(node).startswith(label+' —') and index+1<len(nodes) and text(nodes[index+1]).casefold()==action.casefold():
                tap(nodes[index+1])
                return
        scroll()
    raise AssertionError(f'Offer not found: {label}')

def type_value(hint, value):
    tap(look(hint, max_swipes=7))
    adb('shell', 'input', 'text', value)
    adb('shell', 'input', 'keyevent', '4')

adb('shell','input','keyevent','82')
for setting in ('window_animation_scale','transition_animation_scale','animator_duration_scale'):
    adb('shell','settings','put','global',setting,'0')
device_size=adb('shell','wm','size').splitlines()[-1]
width,height=map(int,re.search(r'(\d+)x(\d+)',device_size).groups())
adb('install','-r',str(APP))
adb('shell','am','start','-W','-n',ACTIVITY)
type_value('Потужність інвертора, кВт','3')
type_value('Ємність акумулятора, А·год','310')
type_value('Напруга системи, В','48')
tap_visible('ВИБРАТИ')
search(lambda n: INVERTER in text(n) and BATTERY in text(n), seconds=45)
print('PASS automatic selection with exact Deye and Dyness pair',flush=True)
tap_visible('Деталі')
search(lambda n: text(n)=='Вибраний енергокомплект')
tap(look('Вихід'))
search(lambda n: 'launcher' in n.get('package',''), seconds=45)
saved=adb('shell','run-as',PACKAGE,'cat','shared_prefs/MainActivity.xml')
assert INVERTER+' + '+BATTERY in saved,'Kit missing from the preferences on disk'
adb('shell','am','force-stop',PACKAGE)
started=adb('shell','am','start','-W','-n',ACTIVITY)
assert 'Error' not in started, started
search(lambda n: n.get('package')==PACKAGE, seconds=45)
tap(look('Порівняти збережені'))
search(lambda n: INVERTER+' + '+BATTERY in text(n))
print('PASS red Exit persists the kit to disk and it appears in comparison after process restart',flush=True)
tap(look('Назад'))
type_value('Потужність інвертора, кВт','3')
type_value('Ємність акумулятора, А·год','310')
type_value('Напруга системи, В','48')
tap_visible('ВИБРАТИ')
search(lambda n: INVERTER in text(n) and BATTERY in text(n), seconds=45)
tap_visible('Деталі')
search(lambda n: text(n)=='Вибраний енергокомплект')
tap(look('PDF / Друк'))
spinner = search(lambda n: n.get('resource-id','').endswith('/destination_spinner'), seconds=45)
if not any(text(n).casefold()=='save as pdf' for n in dump()):
    tap(spinner)
    tap_visible('Save as PDF')
tap(search(lambda n: 'printspooler' in n.get('package','') and n.get('enabled')=='true'
           and (n.get('resource-id','').endswith('/print_button') or
                (n.get('clickable')=='true' and n.get('content-desc','').casefold() in ('save as pdf','save to pdf','save pdf')))))
search(lambda n: 'documentsui' in n.get('package',''), seconds=45)
tap_visible('Save')
for _ in range(20):
    published=adb('shell','find','/sdcard/Download','/sdcard/Documents','-iname','*.pdf',check=False)
    if published:break
    time.sleep(1)
assert published,'Android did not save a PDF in Downloads'
adb('pull',published.splitlines()[0],str(EVIDENCE/'EnergyKit.pdf'))
assert (EVIDENCE/'EnergyKit.pdf').read_bytes().startswith(b'%PDF-')
print('PASS Android printed and saved the PDF',flush=True)
