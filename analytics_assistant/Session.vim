let SessionLoad = 1
let s:so_save = &g:so | let s:siso_save = &g:siso | setg so=0 siso=0 | setl so=-1 siso=-1
let v:this_session=expand("<sfile>:p")
let NvimTreeSetup =  1 
let TabbyTabNames = "{\"4\":\"Terminal\",\"2\":\"BeanConfig\",\"3\":\"Env\",\"1\":\"App\",\"6\":\"Configjson\",\"5\":\"Prompt\"}"
let NvimTreeRequired =  1 
silent only
silent tabonly
cd ~/Repos/analytics-assistant/analytics_assistant
if expand('%') == '' && !&modified && line('$') <= 1 && getline(1) == ''
  let s:wipebuf = bufnr('%')
endif
let s:shortmess_save = &shortmess
if &shortmess =~ 'A'
  set shortmess=aoOA
else
  set shortmess=aoO
endif
badd +32 term://~/Repos/analytics-assistant/analytics_assistant//11850:/usr/bin/fish
badd +1386 term://~/Repos/analytics-assistant/analytics_assistant//6761:/usr/bin/fish
badd +5 src/main/java/com/catgineer/analytics_assistant/infrastructure/ports/DataSourceProvider.java
badd +178 src/main/java/com/catgineer/analytics_assistant/infrastructure/adapters/OpenWebUIAdapter.java
badd +1 src/main/java/com/catgineer/analytics_assistant/infrastructure/adapters/WebDataSourceAdapter.java
badd +2 src/main/java/com/catgineer/analytics_assistant/infrastructure/ports/VisualisationProvider.java
badd +13 src/main/java/com/catgineer/analytics_assistant/infrastructure/ports/AIProvider.java
badd +1 src/main/java/com/catgineer/analytics_assistant/AnalyticsAssistantApplication.java
badd +1 src/main/resources/app_config.json
badd +10 ~/Repos/analytics-assistant/.env
badd +1 ~/Repos/analytics-assistant/.gitignore
badd +84 src/main/java/com/catgineer/analytics_assistant/control/configuration/BeanConfiguration.java
badd +8 ~/Repos/newspipe/.env
badd +1 ~/Repos/pipeline_infra/.env
badd +20 ~/Repos/pipeline_infra/env.auto.tfvars
badd +10 ~/Repos/Cat-Assistant/.env
badd +22 ~/Repos/infra-stuff/fish/config.fish
badd +43 ~/.config/fish/functions/infra.fish
badd +96 src/main/java/com/catgineer/analytics_assistant/domain/services/AIService.java
badd +73 src/main/java/com/catgineer/analytics_assistant/application/services/IngestSources.java
badd +90 src/main/java/com/catgineer/analytics_assistant/control/controllers/AnalyticsController.java
badd +1 term://~/Repos/analytics-assistant/analytics_assistant//49176:/usr/bin/fish
badd +126 src/main/java/com/catgineer/analytics_assistant/infrastructure/adapters/SupersetAdapter.java
badd +7 src/main/java/com/catgineer/analytics_assistant/domain/services/VisualisationService.java
badd +1 src/main/java/com/catgineer/analytics_assistant/application/services/GenerateChartFromPrompt.java
badd +1 src/main/java/com/catgineer/analytics_assistant/domain/model/ChartDataSet.java
badd +15 src/main/java/com/catgineer/analytics_assistant/domain/services/DataSourceService.java
argglobal
%argdel
$argadd NvimTree_1
set stal=2
tabnew +setlocal\ bufhidden=wipe
tabnew +setlocal\ bufhidden=wipe
tabnew +setlocal\ bufhidden=wipe
tabnew +setlocal\ bufhidden=wipe
tabnew +setlocal\ bufhidden=wipe
tabrewind
edit src/main/java/com/catgineer/analytics_assistant/control/configuration/BeanConfiguration.java
let s:save_splitbelow = &splitbelow
let s:save_splitright = &splitright
set splitbelow splitright
wincmd _ | wincmd |
vsplit
1wincmd h
wincmd w
let &splitbelow = s:save_splitbelow
let &splitright = s:save_splitright
wincmd t
let s:save_winminheight = &winminheight
let s:save_winminwidth = &winminwidth
set winminheight=0
set winheight=1
set winminwidth=0
set winwidth=1
exe 'vert 1resize ' . ((&columns * 30 + 79) / 158)
exe 'vert 2resize ' . ((&columns * 127 + 79) / 158)
argglobal
enew
file NvimTree_1
balt src/main/java/com/catgineer/analytics_assistant/domain/services/AIService.java
setlocal foldmethod=manual
setlocal foldexpr=0
setlocal foldmarker={{{,}}}
setlocal foldignore=#
setlocal foldlevel=0
setlocal foldminlines=1
setlocal foldnestmax=20
setlocal nofoldenable
lcd ~/Repos/analytics-assistant/analytics_assistant
wincmd w
argglobal
balt ~/Repos/analytics-assistant/analytics_assistant/src/main/java/com/catgineer/analytics_assistant/infrastructure/adapters/OpenWebUIAdapter.java
setlocal foldmethod=manual
setlocal foldexpr=0
setlocal foldmarker={{{,}}}
setlocal foldignore=#
setlocal foldlevel=0
setlocal foldminlines=1
setlocal foldnestmax=20
setlocal foldenable
silent! normal! zE
let &fdl = &fdl
let s:l = 84 - ((10 * winheight(0) + 15) / 31)
if s:l < 1 | let s:l = 1 | endif
keepjumps exe s:l
normal! zt
keepjumps 84
normal! 025|
lcd ~/Repos/analytics-assistant/analytics_assistant
wincmd w
exe 'vert 1resize ' . ((&columns * 30 + 79) / 158)
exe 'vert 2resize ' . ((&columns * 127 + 79) / 158)
tabnext
edit ~/Repos/analytics-assistant/analytics_assistant/src/main/java/com/catgineer/analytics_assistant/domain/services/DataSourceService.java
let s:save_splitbelow = &splitbelow
let s:save_splitright = &splitright
set splitbelow splitright
wincmd _ | wincmd |
vsplit
1wincmd h
wincmd w
let &splitbelow = s:save_splitbelow
let &splitright = s:save_splitright
wincmd t
let s:save_winminheight = &winminheight
let s:save_winminwidth = &winminwidth
set winminheight=0
set winheight=1
set winminwidth=0
set winwidth=1
wincmd =
argglobal
enew
file ~/Repos/analytics-assistant/analytics_assistant/NvimTree_6
balt ~/Repos/analytics-assistant/analytics_assistant/src/main/java/com/catgineer/analytics_assistant/infrastructure/adapters/OpenWebUIAdapter.java
setlocal foldmethod=manual
setlocal foldexpr=0
setlocal foldmarker={{{,}}}
setlocal foldignore=#
setlocal foldlevel=0
setlocal foldminlines=1
setlocal foldnestmax=20
setlocal nofoldenable
lcd ~/Repos/analytics-assistant/analytics_assistant
wincmd w
argglobal
balt ~/Repos/analytics-assistant/analytics_assistant/src/main/java/com/catgineer/analytics_assistant/domain/services/VisualisationService.java
setlocal foldmethod=manual
setlocal foldexpr=0
setlocal foldmarker={{{,}}}
setlocal foldignore=#
setlocal foldlevel=0
setlocal foldminlines=1
setlocal foldnestmax=20
setlocal foldenable
silent! normal! zE
let &fdl = &fdl
let s:l = 15 - ((14 * winheight(0) + 15) / 31)
if s:l < 1 | let s:l = 1 | endif
keepjumps exe s:l
normal! zt
keepjumps 15
normal! 0
lcd ~/Repos/analytics-assistant/analytics_assistant
wincmd w
2wincmd w
wincmd =
tabnext
edit ~/Repos/analytics-assistant/.env
argglobal
balt ~/.config/fish/functions/infra.fish
setlocal foldmethod=manual
setlocal foldexpr=0
setlocal foldmarker={{{,}}}
setlocal foldignore=#
setlocal foldlevel=0
setlocal foldminlines=1
setlocal foldnestmax=20
setlocal foldenable
silent! normal! zE
let &fdl = &fdl
let s:l = 10 - ((9 * winheight(0) + 15) / 31)
if s:l < 1 | let s:l = 1 | endif
keepjumps exe s:l
normal! zt
keepjumps 10
normal! 0
lcd ~/Repos/analytics-assistant/analytics_assistant
tabnext
argglobal
if bufexists(fnamemodify("term://~/Repos/analytics-assistant/analytics_assistant//6761:/usr/bin/fish", ":p")) | buffer term://~/Repos/analytics-assistant/analytics_assistant//6761:/usr/bin/fish | else | edit term://~/Repos/analytics-assistant/analytics_assistant//6761:/usr/bin/fish | endif
if &buftype ==# 'terminal'
  silent file term://~/Repos/analytics-assistant/analytics_assistant//6761:/usr/bin/fish
endif
balt term://~/Repos/analytics-assistant/analytics_assistant//11850:/usr/bin/fish
setlocal foldmethod=manual
setlocal foldexpr=0
setlocal foldmarker={{{,}}}
setlocal foldignore=#
setlocal foldlevel=0
setlocal foldminlines=1
setlocal foldnestmax=20
setlocal foldenable
let s:l = 266 - ((30 * winheight(0) + 15) / 31)
if s:l < 1 | let s:l = 1 | endif
keepjumps exe s:l
normal! zt
keepjumps 266
normal! 0
lcd ~/Repos/analytics-assistant/analytics_assistant
tabnext
argglobal
if bufexists(fnamemodify("term://~/Repos/analytics-assistant/analytics_assistant//49176:/usr/bin/fish", ":p")) | buffer term://~/Repos/analytics-assistant/analytics_assistant//49176:/usr/bin/fish | else | edit term://~/Repos/analytics-assistant/analytics_assistant//49176:/usr/bin/fish | endif
if &buftype ==# 'terminal'
  silent file term://~/Repos/analytics-assistant/analytics_assistant//49176:/usr/bin/fish
endif
balt term://~/Repos/analytics-assistant/analytics_assistant//6761:/usr/bin/fish
setlocal foldmethod=manual
setlocal foldexpr=0
setlocal foldmarker={{{,}}}
setlocal foldignore=#
setlocal foldlevel=0
setlocal foldminlines=1
setlocal foldnestmax=20
setlocal foldenable
let s:l = 39 - ((30 * winheight(0) + 15) / 31)
if s:l < 1 | let s:l = 1 | endif
keepjumps exe s:l
normal! zt
keepjumps 39
normal! 0
lcd ~/Repos/analytics-assistant/analytics_assistant
tabnext
edit ~/Repos/analytics-assistant/analytics_assistant/src/main/resources/app_config.json
argglobal
balt term://~/Repos/analytics-assistant/analytics_assistant//6761:/usr/bin/fish
setlocal foldmethod=manual
setlocal foldexpr=0
setlocal foldmarker={{{,}}}
setlocal foldignore=#
setlocal foldlevel=0
setlocal foldminlines=1
setlocal foldnestmax=20
setlocal foldenable
silent! normal! zE
let &fdl = &fdl
let s:l = 1 - ((0 * winheight(0) + 15) / 31)
if s:l < 1 | let s:l = 1 | endif
keepjumps exe s:l
normal! zt
keepjumps 1
normal! 0
lcd ~/Repos/analytics-assistant/analytics_assistant
tabnext 2
set stal=1
if exists('s:wipebuf') && len(win_findbuf(s:wipebuf)) == 0 && getbufvar(s:wipebuf, '&buftype') isnot# 'terminal'
  silent exe 'bwipe ' . s:wipebuf
endif
unlet! s:wipebuf
set winheight=1 winwidth=20
let &shortmess = s:shortmess_save
let s:sx = expand("<sfile>:p:r")."x.vim"
if filereadable(s:sx)
  exe "source " . fnameescape(s:sx)
endif
let &g:so = s:so_save | let &g:siso = s:siso_save
set hlsearch
doautoall SessionLoadPost
unlet SessionLoad
" vim: set ft=vim :
