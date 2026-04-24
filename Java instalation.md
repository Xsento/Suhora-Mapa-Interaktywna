Setup Javy dla VS Code na Windows, kroki na Linuxie mogą się różnić

# 1. Pobierz i zainstaluj Javę
  - wejdź na stronę https://adoptium.net i pobierz instalator
  - przeklikaj instalator z domyślnymi opcjami

# 2. Konfiguracja zmiennych środowiskowych

### Krok 1: Znajdź folder swojej Javy

Zazwyczaj Java instaluje się w jednym z tych miejsc:

- `C:\Program Files\Eclipse Adoptium\jdk-17.x.x...`
- `C:\Program Files\Java\jdk-21...`
    
Wejdź tam i skopiuj ścieżkę do głównego folderu (tego, który zawiera folder `bin`, ale **nie wchodź** do środka `bin`).

### Krok 2: Ustaw Zmienną Środowiskową

1. Naciśnij klawisz **Windows** i wpisz: **"Zmienne środowiskowe"** (lub "Edit the system environment variables").
2. Kliknij przycisk **Zmienne środowiskowe...** (Environment Variables) na dole.
3. W dolnej sekcji (**Zmienne systemowe**) kliknij **Nowa...**.
4. Wpisz:
    - Nazwa zmiennej: `JAVA_HOME`
    - Wartość zmiennej: (Wklej ścieżkę, którą skopiowałeś w Kroku 1).
5. Kliknij OK.
    

### Krok 3: Dodaj do Path (ważne!)

1. Na tej samej liście (Zmienne systemowe) znajdź zmienną o nazwie **Path** i kliknij **Edytuj**.
2. Kliknij **Nowy** i wpisz: `%JAVA_HOME%\bin`
3. Kliknij OK we wszystkich okienkach.
    
# 3. Konfiguracja VS Code

W samym VS Code wejdź w zakładkę Extensions (Ctrl+Shift+X) i zainstaluj:
Extension Pack for Java (od Microsoftu)

