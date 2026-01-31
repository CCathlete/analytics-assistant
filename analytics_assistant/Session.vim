let SessionLoad = 1
let s:so_save = &g:so | let s:siso_save = &g:siso | setg so=0 siso=0 | setl so=-1 siso=-1
let v:this_session=expand("<sfile>:p")
let NvimTreeSetup =  1 
let TabbyTabNames = "{\"2\":\"Gemini\",\"1\":\"App\",\"3\":\"Terminal\"}"
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
badd +22 term://~/Repos/analytics-assistant/analytics_assistant//6650:/usr/bin/fish
badd +218 term://~/Repos/analytics-assistant/analytics_assistant//6761:/usr/bin/fish
badd +1 src/main/java/com/catgineer/analytics_assistant/infrastructure/ports/DataSourceProvider.java
argglobal
%argdel
$argadd NvimTree_1
set stal=2
tabnew +setlocal\ bufhidden=wipe
tabnew +setlocal\ bufhidden=wipe
tabrewind
edit src/main/java/com/catgineer/analytics_assistant/infrastructure/ports/DataSourceProvider.java
argglobal
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
let s:l = 5 - ((4 * winheight(0) + 16) / 32)
if s:l < 1 | let s:l = 1 | endif
keepjumps exe s:l
normal! zt
keepjumps 5
normal! 019|
lcd ~/Repos/analytics-assistant/analytics_assistant
tabnext
argglobal
if bufexists(fnamemodify("term://~/Repos/analytics-assistant/analytics_assistant//6650:/usr/bin/fish", ":p")) | buffer term://~/Repos/analytics-assistant/analytics_assistant//6650:/usr/bin/fish | else | edit term://~/Repos/analytics-assistant/analytics_assistant//6650:/usr/bin/fish | endif
if &buftype ==# 'terminal'
  silent file term://~/Repos/analytics-assistant/analytics_assistant//6650:/usr/bin/fish
endif
setlocal foldmethod=manual
setlocal foldexpr=0
setlocal foldmarker={{{,}}}
setlocal foldignore=#
setlocal foldlevel=0
setlocal foldminlines=1
setlocal foldnestmax=20
setlocal foldenable
let s:l = 22 - ((21 * winheight(0) + 16) / 32)
if s:l < 1 | let s:l = 1 | endif
keepjumps exe s:l
normal! zt
keepjumps 22
normal! 0
lcd ~/Repos/analytics-assistant/analytics_assistant
tabnext
argglobal
if bufexists(fnamemodify("term://~/Repos/analytics-assistant/analytics_assistant//6761:/usr/bin/fish", ":p")) | buffer term://~/Repos/analytics-assistant/analytics_assistant//6761:/usr/bin/fish | else | edit term://~/Repos/analytics-assistant/analytics_assistant//6761:/usr/bin/fish | endif
if &buftype ==# 'terminal'
  silent file term://~/Repos/analytics-assistant/analytics_assistant//6761:/usr/bin/fish
endif
balt term://~/Repos/analytics-assistant/analytics_assistant//6650:/usr/bin/fish
setlocal foldmethod=manual
setlocal foldexpr=0
setlocal foldmarker={{{,}}}
setlocal foldignore=#
setlocal foldlevel=0
setlocal foldminlines=1
setlocal foldnestmax=20
setlocal foldenable
let s:l = 1 - ((0 * winheight(0) + 16) / 32)
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
nohlsearch
doautoall SessionLoadPost
unlet SessionLoad
" vim: set ft=vim :
