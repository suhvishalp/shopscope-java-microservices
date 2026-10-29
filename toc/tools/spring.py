import openpyxl
from openpyxl.styles import Font, PatternFill, Alignment, Border, Side
from openpyxl.utils import get_column_letter
from openpyxl.worksheet.datavalidation import DataValidation
from openpyxl.formatting.rule import FormulaRule
NAVY, CODE, BRK, NEW = '1F3864', 'DCE6F1', 'F2F2F2', 'FFF2CC'
BLUE = '0000FF'
thin = Side(style='thin', color='BFBFBF'); B = Border(bottom=thin, top=thin, left=thin, right=thin)
F = lambda **k: Font(name='Arial', **k)
fill = lambda c: PatternFill('solid', fgColor=c)
DEF_PAT = [None, 'Break', 'Lunch', 'Break']
OUT = '/Users/vishalshah/personal/shopscope-java-microservices/toc/Spring-Boot-Microservices-5-Day-TOC-with-Add-ons.xlsx'
def S(code, mins, demo, title, topics, labs, new=False):
    return dict(code=code, mins=mins, demo=demo, title=title, topics=topics, labs=labs, new=new)
import os; exec(open(os.path.join(os.path.dirname(os.path.abspath(__file__)), 'spring_data.py')).read())
# WRITE
wb = openpyxl.Workbook()
NOTES = 'Settings and notes'
BRK_CELL, LUNCH_CELL, START_CELL = f"'{NOTES}'!$C$5", f"'{NOTES}'!$C$6", f"'{NOTES}'!$C$4"


def title_rows(ws, title, blurb, n, note=None):
    ws.merge_cells(start_row=1, start_column=1, end_row=1, end_column=n)
    c = ws.cell(1, 1, title); c.font = F(sz=16, b=True, color='FFFFFF'); c.fill = fill(NAVY); c.alignment = Alignment(vertical='center', indent=1)
    ws.row_dimensions[1].height = 30
    ws.merge_cells(start_row=2, start_column=1, end_row=2, end_column=n)
    c = ws.cell(2, 1, blurb); c.font = F(sz=10, color='404040'); c.alignment = Alignment(wrap_text=True, vertical='top')
    ws.row_dimensions[2].height = 34
    if note:
        ws.merge_cells(start_row=3, start_column=1, end_row=3, end_column=n)
        c = ws.cell(3, 1, note); c.font = F(sz=10, i=True, color='7B3F00'); c.alignment = Alignment(wrap_text=True, vertical='top')
        ws.row_dimensions[3].height = 28

def header(ws, row, cols):
    for i, h in enumerate(cols, 1):
        c = ws.cell(row, i, h); c.font = F(sz=10, b=True, color='FFFFFF'); c.fill = fill(NAVY); c.border = B
        c.alignment = Alignment(wrap_text=True, vertical='center')
    ws.row_dimensions[row].height = 24

def band(ws, row, text, n, color=NAVY):
    ws.merge_cells(start_row=row, start_column=1, end_row=row, end_column=n)
    c = ws.cell(row, 1, text); c.font = F(sz=11, b=True, color='FFFFFF'); c.fill = fill(color); c.alignment = Alignment(vertical='center', indent=1)
    ws.row_dimensions[row].height = 20

def page(ws, freeze):
    ws.freeze_panes = freeze; ws.page_setup.orientation = 'landscape'; ws.sheet_properties.pageSetUpPr.fitToPage = True
    ws.page_setup.fitToWidth = 1; ws.page_setup.fitToHeight = 0

lines = lambda t: (t or '').count('\n') + 1
COLS = ['Session', 'Start', 'End', 'Session title', 'Demo', 'Topics covered', 'Labs covered']
WIDTH = {'A': 9, 'B': 7.5, 'C': 7.5, 'D': 34, 'E': 17, 'F': 66, 'G': 46}

def session_row(ws, r, s, start):
    h, m = divmod(s['mins'], 60)
    v = [s['code'], start, f'=B{r}+TIME({h},{m},0)', s['title'], s['demo'], s['topics'], s['labs']]
    for i, val in enumerate(v, 1):
        c = ws.cell(r, i, val); c.border = B; c.alignment = Alignment(wrap_text=True, vertical='top'); c.font = F(sz=10, b=(i in (1, 4)))
    ws.cell(r, 1).fill = fill(CODE); ws.cell(r, 5).fill = fill(NEW if s.get('new') else CODE)
    if s.get('new'): ws.cell(r, 4).fill = fill(NEW)
    ws.cell(r, 2).number_format = ws.cell(r, 3).number_format = 'hh:mm'
    ws.row_dimensions[r].height = max(15 * lines(s['labs']), 15 * (len(s['topics']) // 100 + 1), 45)

def break_row(ws, r, label):
    ref = LUNCH_CELL if label == 'Lunch' else BRK_CELL
    v = [label, f'=C{r-1}', f'=B{r}+{ref}/1440']
    for i in range(1, 8):
        c = ws.cell(r, i, v[i-1] if i <= 3 else None); c.fill = fill(BRK); c.border = B; c.font = F(sz=10, color='808080')
    ws.cell(r, 2).number_format = ws.cell(r, 3).number_format = 'hh:mm'

def write_day(ws, r, sessions, pattern):
    header(ws, r, COLS); r += 1; rows = []
    for i, s in enumerate(sessions):
        if i and pattern[i]: break_row(ws, r, pattern[i]); r += 1
        session_row(ws, r, s, f'={START_CELL}' if i == 0 else f'=C{r-1}'); rows.append(r); r += 1
    return rows, r

# ---- settings (inputs referenced by every time) ----
notes = wb.active; notes.title = NOTES
notes['B4'] = 'Day starts at'; notes['C4'] = '=TIME(9,0,0)'
notes['B5'] = 'Short break (minutes)'; notes['C5'] = 15
notes['B6'] = 'Lunch (minutes)'; notes['C6'] = 60

# ---- core day sheets ----
day_rows = []
for d in CORE:
    ws = wb.create_sheet('Core ' + d['short'])
    for k, w in WIDTH.items(): ws.column_dimensions[k].width = w
    title_rows(ws, d['name'], d['blurb'], 7, d.get('note'))
    rows, _ = write_day(ws, 5, d['sessions'], DEF_PAT)
    page(ws, 'A6'); day_rows.append((d, ws.title, rows))

# ---- 5-day core at a glance ----
g = wb.create_sheet('5-day core')
for k, w in {'A': 9, 'B': 7.5, 'C': 7.5, 'D': 34, 'E': 17, 'F': 72, 'G': 40}.items(): g.column_dimensions[k].width = w
title_rows(g, CORE_TITLE, CORE_BLURB, 7)
header(g, 4, COLS); r = 5
for d, sheet, rows in day_rows:
    band(g, r, d['name'], 7); r += 1
    for sr in rows:
        q = f"'{sheet}'!"
        for i, col in enumerate('ABCDEFG', 1):
            c = g.cell(r, i, f'={q}{col}{sr}'); c.border = B; c.alignment = Alignment(wrap_text=True, vertical='top')
            c.font = F(sz=9, b=(i in (1, 4)), color='008000' if i in (2, 3) else None)
        g.cell(r, 1).fill = fill(CODE); g.cell(r, 5).fill = fill(CODE)
        g.cell(r, 2).number_format = g.cell(r, 3).number_format = 'hh:mm'; g.row_dimensions[r].height = 62; r += 1
    r += 1
page(g, 'A5')

# ---- add-on days ----
ad = wb.create_sheet('Add-on days')
for k, w in WIDTH.items(): ad.column_dimensions[k].width = w
title_rows(ad, 'Add-on days · each one adds exactly one day',
           ADDON_BLURB, 7)
r = 4
for a in ADDONS:
    band(ad, r, f"{a['code']} · {a['name']}", 7, '5B6478'); r += 1
    for lab, val in (('When', a['place']), ('Requires', a['requires']), ('Why add it', a['why'])):
        ad.cell(r, 1, lab).font = F(sz=9, b=True, color='404040')
        ad.merge_cells(start_row=r, start_column=2, end_row=r, end_column=7)
        c = ad.cell(r, 2, val); c.font = F(sz=9, color='404040'); c.alignment = Alignment(wrap_text=True, vertical='top'); r += 1
    _, r = write_day(ad, r, a['sessions'], a['pattern']); r += 1
page(ad, 'A4')

# ---- Build your programme (the client's selection sheet) ----
bp = wb.create_sheet('Build your programme', 0)
for k, w in {'A': 11, 'B': 50, 'C': 70, 'D': 34, 'E': 13}.items(): bp.column_dimensions[k].width = w
title_rows(bp, 'Build your programme',
           'The five core days are always included. Add any of the optional days by choosing Yes in the yellow cells: each one adds exactly one day. Your day-by-day programme below updates as you choose.', 5)
header(bp, 4, ['Day', 'Title', 'What your team will be able to do', 'Requires', 'Include?'])
band(bp, 5, 'The core · always included', 5)
r = 6; 
for i, d in enumerate(CORE):
    vals = [d['short'], d['name'].split('— ')[-1], CORE_PITCH[i], '—', 'Included']
    for j, v in enumerate(vals, 1):
        c = bp.cell(r, j, v); c.border = B; c.font = F(sz=10, b=(j <= 2)); c.alignment = Alignment(wrap_text=True, vertical='top')
    bp.cell(r, 1).fill = fill(CODE); bp.cell(r, 5).font = F(sz=10, color='808080'); bp.row_dimensions[r].height = 30; r += 1
band(bp, r, 'Optional add-on days · choose Yes or No', 5, '5B6478'); r += 1
dv = DataValidation(type='list', formula1='"Yes,No"', allow_blank=False, showDropDown=False)
bp.add_data_validation(dv); inc = {}
for a in ADDONS:
    vals = [a['code'], a['name'], a['pitch'], a['requires'], 'No']
    for j, v in enumerate(vals, 1):
        c = bp.cell(r, j, v); c.border = B; c.font = F(sz=10, b=(j <= 2)); c.alignment = Alignment(wrap_text=True, vertical='top')
    bp.cell(r, 1).fill = fill(ADD_COL[a['short']])
    e = bp.cell(r, 5); e.fill = fill('FFFF00'); e.font = F(sz=11, b=True, color=BLUE); e.alignment = Alignment(horizontal='center', vertical='center')
    dv.add(e); inc[a['code']] = f'$E${r}'; bp.row_dimensions[r].height = 32; r += 1
r += 1
bp.cell(r, 1, 'Total').font = F(sz=11, b=True)
c = bp.cell(r, 2, f'=5+COUNTIF(E{r-1-len(ADDONS)}:E{r-2},"Yes")&" training days"'); c.font = F(sz=12, b=True, color=NAVY)
tot_row = r; r += 1
for need, dep, msg in WARN:
    c = bp.cell(r, 2, f'=IF(AND({inc[need]}="Yes",{inc[dep]}<>"Yes"),"{msg}","")'); c.font = F(sz=10, b=True, color='C00000'); r += 1
r += 1

# helper table (hidden columns H:K): every possible day in its fixed order, with a running day number
HCOL = 8; h0 = 5
helper = [('Pre-course', inc['Pre-course'], ADDONS[0]['code'] + ' · ' + ADDONS[0]['name'], 'Add-on days', 'Bridge')]
helper += [(d['short'], '"Yes"', f"Core · {d['name'].split('— ')[-1]}", 'Core ' + d['short'], 'Core') for d in CORE]
helper += [(a['code'], inc[a['code']], f"{a['code']} · {a['name']}", 'Add-on days', a['short']) for a in ADDONS[1:]]
for k, (code, flag, title, where, kind) in enumerate(helper):
    hr = h0 + k
    bp.cell(hr, HCOL, f'={flag}' if flag.startswith('$') else 'Yes')
    bp.cell(hr, HCOL + 1, f'=IF(H{hr}="Yes",1,0)' if k == 0 else f'=J{hr-1}+IF(H{hr}="Yes",1,0)')
    bp.cell(hr, HCOL + 1).number_format = '0'
    bp.cell(hr, HCOL + 2, title); bp.cell(hr, HCOL + 3, where); bp.cell(hr, HCOL + 4, kind)
# wait: running count lives in column I (HCOL+1); fix references
for k in range(len(helper)):
    hr = h0 + k
    bp.cell(hr, HCOL + 1, f'=IF(H{hr}="Yes",1,0)' if k == 0 else f'=I{hr-1}+IF(H{hr}="Yes",1,0)')
hl = h0 + len(helper) - 1
for col in 'HIJKL': bp.column_dimensions[col].hidden = True

band(bp, r, 'Your programme, day by day', 5); r += 1
header(bp, r, ['Day', 'Title', 'Details on sheet', '', '']); r += 1
first_sched = r
for k in range(1, len(helper) + 1):
    m = f'MATCH({k},$I${h0}:$I${hl},0)'
    bp.cell(r, 1, f'=IF(ISNA({m}),"","Day {k}")')
    bp.cell(r, 2, f'=IFERROR(INDEX($J${h0}:$J${hl},{m}),"")')
    bp.cell(r, 3, f'=IFERROR(INDEX($K${h0}:$K${hl},{m}),"")')
    bp.cell(r, 12, f'=IFERROR(INDEX($L${h0}:$L${hl},{m}),"")')   # kind, drives the colour
    for j in (1, 2, 3):
        c = bp.cell(r, j); c.font = F(sz=10, b=(j == 1)); c.alignment = Alignment(vertical='top')
    r += 1
rng = f'A{first_sched}:C{r-1}'
bp.conditional_formatting.add(rng, FormulaRule(formula=[f'$L{first_sched}="Core"'], fill=fill(CODE), border=B))
for kind, col in ADD_COL.items():
    bp.conditional_formatting.add(rng, FormulaRule(formula=[f'$L{first_sched}="{kind}"'], fill=fill(col), border=B))
bp.freeze_panes = 'A5'; bp.page_setup.orientation = 'landscape'; bp.sheet_properties.pageSetUpPr.fitToPage = True
bp.page_setup.fitToWidth = 1; bp.page_setup.fitToHeight = 0

# ---- Example packages: one row per package, one coloured cell per day ----
pk = wb.create_sheet('Example packages')
pk.column_dimensions['A'].width = 12; pk.column_dimensions['B'].width = 34
for k in range(NDAYS): pk.column_dimensions[get_column_letter(3 + k)].width = 12
title_rows(pk, 'Example packages · what each extra day adds',
           'Each row is a complete programme, read left to right. Blue cells are the five core days; every coloured cell is one add-on day. These are suggestions: any combination can be built on the "Build your programme" sheet.', 2 + NDAYS)
r = 4
pk.cell(r, 1, 'Key').font = F(sz=10, b=True)
keys = [('Core', CODE, 'Core Days 1–5')] + [(a['short'], ADD_COL[a['short']], f"{a['code']}: {a['name']}") for a in ADDONS]
for i, (lab, col, full) in enumerate(keys):
    c = pk.cell(r + i, 2, full); c.font = F(sz=9); c.fill = fill(col); c.border = B
r += len(keys) + 1
header(pk, r, ['Days', 'Best for'] + [f'Day {k}' for k in range(1, NDAYS + 1)]); r += 1
CORE5 = ['Core 1', 'Core 2', 'Core 3', 'Core 4', 'Core 5']
for n, best, days in PK:
    c = pk.cell(r, 1, f'{n} days'); c.font = F(sz=11, b=True); c.border = B; c.alignment = Alignment(vertical='center')
    c = pk.cell(r, 2, best); c.font = F(sz=9); c.border = B; c.alignment = Alignment(wrap_text=True, vertical='center')
    for k, dname in enumerate(days):
        c = pk.cell(r, 3 + k, dname); c.border = B; c.font = F(sz=9, b=dname not in CORE5)
        c.fill = fill(CODE if dname in CORE5 else ADD_COL[dname]); c.alignment = Alignment(horizontal='center', vertical='center', wrap_text=True)
    pk.row_dimensions[r].height = 30; r += 1
r += 1
pk.merge_cells(start_row=r, start_column=1, end_row=r, end_column=2 + NDAYS)
c = pk.cell(r, 1, ORDER_RULES)
c.font = F(sz=9, color='404040'); c.alignment = Alignment(wrap_text=True)
page(pk, 'A4')

# ---- Settings and notes ----
n = notes
for k, w in {'A': 3, 'B': 30, 'C': 12, 'D': 90}.items(): n.column_dimensions[k].width = w
title_rows(n, 'Settings and notes', 'Change the three yellow cells to match your working day: every start and end time in this file follows them.', 4)
for rr in (4, 5, 6):
    n.cell(rr, 2).font = F(sz=10, b=True); n.cell(rr, 3).font = F(sz=10, color=BLUE); n.cell(rr, 3).fill = fill('FFFF00'); n.cell(rr, 3).border = B
n['C4'].number_format = 'hh:mm'
n['D4'] = 'Default 09:00.'; n['D5'] = 'Two short breaks a day.'; n['D6'] = 'One lunch break a day.'
for rr in (4, 5, 6): n.cell(rr, 4).font = F(sz=9, color='404040')
r = 8
for k, v in NOTE_LINES:
    n.cell(r, 2, k).font = F(sz=10, b=True); n.cell(r, 2).alignment = Alignment(vertical='top', wrap_text=True)
    n.merge_cells(start_row=r, start_column=3, end_row=r, end_column=4)
    c = n.cell(r, 3, v); c.font = F(sz=10); c.alignment = Alignment(wrap_text=True, vertical='top'); n.row_dimensions[r].height = 42; r += 1

order = ['Build your programme', '5-day core', 'Core Day 1', 'Core Day 2', 'Core Day 3', 'Core Day 4', 'Core Day 5', 'Add-on days', 'Example packages', NOTES]
wb._sheets = [wb[x] for x in order]; wb.active = 0
wb.calculation.fullCalcOnLoad = True
import sys; out = sys.argv[1] if len(sys.argv) > 1 else OUT
wb.save(out); print('saved', out)
for a in ADDONS: print(a['code'], sum(s['mins'] for s in a['sessions']), len(a['sessions']), len(a['pattern']))
