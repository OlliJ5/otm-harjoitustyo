package budjetointisovellus.domain;

import budjetointisovellus.dao.BudgetDao;
import budjetointisovellus.dao.ExpenseDao;
import budjetointisovellus.dao.UserDao;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javafx.scene.control.TextField;
import org.mindrot.jbcrypt.*;

/**
 *
 * Luokka vastaa sovelluslogiikasta
 */
public class BudgetService {

    private ExpenseDao expenseDao;
    private UserDao userDao;
    private BudgetDao budgetDao;

    /**
     *
     * @param expenseDao
     * @param userDao
     * @param budgetDao
     */
    public BudgetService(ExpenseDao expenseDao, UserDao userDao, BudgetDao budgetDao) {
        this.expenseDao = expenseDao;
        this.userDao = userDao;
        this.budgetDao = budgetDao;
    }

    /**
     * Uuden käyttäjän luominen
     *
     * @param username luotavan käyttäjän käyttäjätunnus
     * @param name luotavan käyttäjän nimi
     * @param password luotavan käyttäjän salasana
     * @return true
     */
    public boolean createUser(String username, String name, String password) {

        try {
            if (userDao.usernameExists(username)) {
                return false;
            }
        } catch (Exception e) {
            return false;
        }
        String pwHash = BCrypt.hashpw(password, BCrypt.gensalt());
        User user = new User(username, name, pwHash);

        try {
            userDao.create(user);
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    /**
     * Sisäänkirjautuminen
     *
     * @param username käyttäjätunnus
     * @param password salasana
     * @return true, jos käyttäjätunnus on olemassa. False, jos käyttäjätunnusta
     * ei ole
     */
    public boolean login(String username, String password) {
        try {
            if (userDao.usernameAndPasswordCorrect(username, password)) {
                return true;
            }
        } catch (Exception e) {
        }
        return false;
    }

    /**
     * Hakee käyttäjän käyttäjätunnuksen avulla
     *
     * @param username käyttäjätunnus
     * @return User-olio, joka vastaa käyttäjätunnusta
     */
    public User getUser(String username) {
        try {
            return userDao.findByUsername(username);
        } catch (Exception e) {
        }
        return null;
    }

    /**
     * Budjetin luonti
     *
     * @param budget Luotava budjetti oliona
     * @param user Käyttäjä, jolle budjetti luodaan
     * @return true, jos budjetti saadaan luotua. False, jos budjetin luonti ei
     * onnistu
     */
    public boolean createBudget(Budget budget, User user) {
        try {
            if (budgetDao.budgetExists(budget, user.getUsername())) {
                return false;
            }
        } catch (Exception e) {
            return false;
        }

        try {
            budgetDao.create(budget, user.getUsername());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Uuden kulun luominen
     *
     * @param username Käyttäjätunnus, jonka budjettiin kulu luodaan
     * @param budgetName Budjetti, johon kulu luodaan
     * @param expenseName Kulun nimi
     * @param price Kulun hinta
     * @return true, jos luonti onnistuu. False, jos luonti ei onnistu
     */
    public boolean createExpense(String username, String budgetName, String expenseName, Double price) {
        try {
            int id = budgetDao.getIdByNameAndUsername(username, budgetName);
            expenseDao.create(id, expenseName, price);
        } catch (Exception e) {
            return false;
        }

        return true;
    }

    /**
     * Etsii budjettin kuuluvat kulut
     *
     * @param budgetName Budjetin nimi
     * @param username Käyttäjätunnus, jonka budjetin kuluja etsitään
     * @return Palauttaa listan kuluja eli Expense-olioita.
     */
    public List<Expense> findBudgetsExpenses(String budgetName, String username) {
        List<Expense> expenses = new ArrayList<>();
        try {
            int id = budgetDao.getIdByNameAndUsername(username, budgetName);
            expenses = expenseDao.getAllFromABudget(id);
        } catch (Exception e) {
        }
        return expenses;
    }

    /**
     * Etsii käyttäjän budjetit
     *
     * @param user käyttäjä
     * @return Lista budjetteja eli Budget-olioita.
     */
    public List<Budget> findBudgets(User user) {
        List<Budget> budgets = new ArrayList<>();
        try {
            budgets = budgetDao.findBudgetsByUsername(user.getUsername());
        } catch (Exception e) {
        }
        return budgets;
    }

    /**
     * Etsii budjetin budjetin nimen perusteella
     *
     * @param budgetName budjetin nimi
     * @param user käyttäjä, jonka budjettia etsitään
     * @return Budget-olio, joka vastaa budjetin nimeä ja käyttäjää
     */
    public Budget getBudgetByName(String budgetName, User user) {
        try {
            return budgetDao.findOne(budgetName, user.getUsername());
        } catch (Exception e) {
        }
        return null;
    }

    /**
     * Budjetin poistaminen
     *
     * @param budget poistettava budjetti
     * @param user käyttäjä, jonka budjetti poistetaan
     * @return true, jos poistaminen onnistuu, muuten false
     */
    public boolean deleteBudget(Budget budget, User user) {
        try {
            int id = budgetDao.getIdByNameAndUsername(user.getUsername(), budget.getName());
            budgetDao.delete(budget, user.getUsername());
            expenseDao.deleteExpensesFromBudget(id);
            return true;
        } catch (Exception e) {
        }
        return false;
    }

    /**
     * Kulun poistaminen
     *
     * @param budget Budjetti, jonka kulu halutaan poistaa
     * @param user Käyttäjä, jonka budjetti on
     * @param expense Poistettava kulu
     * @return true, jos poista onnistuu, muuten false
     */
    public boolean deleteExpense(Budget budget, User user, Expense expense) {
        try {
            int id = budgetDao.getIdByNameAndUsername(user.getUsername(), budget.getName());
            expenseDao.delete(id, expense);
            return true;
        } catch (Exception e) {
        }
        return false;
    }

    /**
     * Kulujen hinnan laskeminen
     *
     * @param expenses Lista kuluja
     * @return kulujen yhteinen hinnan
     */
    public double totalExpenses(List<Expense> expenses) {
        double total = 0;
        for (int i = 0; i < expenses.size(); i++) {
            total += expenses.get(i).getPrice();
        }
        return total;
    }

    /**
     * Käyttäjän ja siihen liittyvien budjettien sekä kulujen poistaminen
     *
     * @param user käyttäjä, joka halutaan poistaa
     * @return true, jos poisto onnistuu, muuten false
     */
    public boolean deleteUser(User user) {
        try {
            this.deleteUserExpenses(user);
            budgetDao.deleteBudgetsOfUser(user);
            userDao.delete(user);
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    /**
     * Poistaa käyttäjään liittyvät kulut
     *
     * @param user käyttäjä, jonka kulut halutaan poistaa
     * @return true, jos poisto onnistuu, muuten false
     */
    public boolean deleteUserExpenses(User user) {
        try {
            List<Budget> budgets = this.findBudgets(user);
            for (int i = 0; i < budgets.size(); i++) {
                int id = budgetDao.getIdByNameAndUsername(user.getUsername(), budgets.get(i).getName());
                expenseDao.deleteExpensesFromBudget(id);
            }
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    /**
     * Budjetin nimen päivitys
     *
     * @param newName nimi, joka budjetille halutaan asettaa
     * @param user käyttäjä, jolle budjetti kuuluu
     * @param oldName budjetin vanha nimi, eli nimi, joka halutaan muuttaa
     * @return true, jos muutos onnistuu, muuten false
     */
    public boolean updateBudgetName(String newName, User user, String oldName) {
        try {
            if (budgetDao.budgetExists(new Budget(newName, 0), user.getUsername())) {
                return false;
            }
            int id = budgetDao.getIdByNameAndUsername(user.getUsername(), oldName);
            budgetDao.updateBudgetName(id, newName);
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    /**
     * Budjetin määrän päivitys
     *
     * @param amount määrä, joka budjetille halutaan asettaa
     * @param user käyttäjä, jolle budjetti kuuluu
     * @param budgetName budjetin, jonka määrää halutaan muuttaa, nimi
     * @return true, jos muutos onnistuu, muuten false
     */
    public boolean updateBudgetAmount(Double amount, User user, String budgetName) {
        try {
            int id = budgetDao.getIdByNameAndUsername(user.getUsername(), budgetName);
            budgetDao.updateBudgetAmount(id, amount);
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    /**
     * Tarkistaa onko luku liukuluku
     *
     * @param message Tekstikentän sisältö
     * @return True, jos luku on liukuluku, muuten false
     */
    public boolean isDouble(String message) {
        try {
            Double number = Double.parseDouble(message);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

}
