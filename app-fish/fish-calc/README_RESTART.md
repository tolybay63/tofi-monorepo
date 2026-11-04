# Памятка по управлению микросервисами комплекса Fish

Рабочая директория:
- **Ubuntu (`systemd`):** `/var/www/fish/backend`
- **Windows (`NSSM`):** `C:\fish\backend`

## 1. Сводная таблица скриптов и служб

| Модуль (Папка) | Служба Ubuntu (`systemd`) | Служба Windows (`NSSM`) | Скрипт Ubuntu (`.sh`) | Скрипт Windows (`.bat`) |
| :--- | :--- | :--- | :--- | :--- |
| **meta** | `fish-meta.service` | `fish-meta` | `./restart_meta.sh` | `.\restart_meta.bat` |
| **nsi** | `fish-nsi.service` | `fish-nsi` | `./restart_nsi.sh` | `.\restart_nsi.bat` |
| **admin** | `fish-admin.service` | `fish-admin` | `./restart_admin.sh` | `.\restart_admin.bat` |
| **monitoring** | `fish-monitoring.service` | `fish-monitoring` | `./restart_monitoring.sh` | `.\restart_monitoring.bat` |
| **calc** | `fish-calc.service` | `fish-calc` | `./restart_calc.sh` | `.\restart_calc.bat` |
| **calc-fastapi** | `fish-calc-fastapi.service` | `fish-calc-fastapi` | `./restart_calc_fastapi.sh` | `.\restart_calc_fastapi.bat` |
| **fishstock** | `fishstock.service` | `fishstock` | `./restart_fishstock.sh` | `.\restart_fishstock.bat` |
| **Nginx** | `nginx.service` | `nginx-service` | `systemctl restart nginx` | `.\restart_nginx.bat` |
| **Весь стек** | Все `fish*` + `nginx` | Все `fish*` + `nginx-service` | `./restart_all.sh` | `.\restart_all.bat` |

## 2. Регламент обновления модулей

### Ubuntu (`/var/www/fish/backend`)
1. Скопируйте новые файлы в папку модуля.
2. Запустите соответствующий скрипт `./restart_<модуль>.sh` (или `./restart_all.sh`).

### Windows (`C:\fish\backend`)
1. Скопируйте новые файлы в папку модуля.
2. Запустите `.\restart_<модуль>.bat` (или `.\restart_all.bat`) из Far Manager, Проводника или PowerShell.

## 3. Команды диагностики и просмотра логов

### Ubuntu
- Статус всех служб:
  `systemctl list-units --type=service "fish*" "nginx.service" --no-pager`
- Просмотр логов сервиса в реальном времени:
  `journalctl -u fish-meta.service -f -n 50`

### Windows (PowerShell 7)
- Статус всех служб:
  `Get-Service -Name 'fish*','nginx-service' | Format-Table Status, Name, DisplayName -AutoSize`
- Редактирование параметров службы в NSSM:
  `nssm edit fish-meta`
- Обновление `pip` в `calc-fastapi`:
  `C:\fish\backend\calc-fastapi\.venv\Scripts\python.exe -m pip install --upgrade pip`