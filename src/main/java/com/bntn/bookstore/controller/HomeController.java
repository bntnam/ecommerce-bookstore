package com.bntn.bookstore.controller;

import com.bntn.bookstore.model.*;
import com.bntn.bookstore.model.security.PasswordResetToken;
import com.bntn.bookstore.model.security.Role;
import com.bntn.bookstore.model.security.UserRole;
import com.bntn.bookstore.service.*;
import com.bntn.bookstore.service.impl.UserSecurityService;
import com.bntn.bookstore.utility.MailConstructor;
import com.bntn.bookstore.utility.PLConstants;
import com.bntn.bookstore.utility.SecurityUtility;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.websocket.server.PathParam;
import java.security.Principal;
import java.util.*;

@Controller
public class HomeController {

    @Autowired
    private UserService userService;

    @Autowired
    private UserSecurityService userSecurityService;

    @Autowired
    private BookService bookService;

    @Autowired
    private UserPaymentService userPaymentService;

    @Autowired
    private UserShippingService userShippingService;

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private MailConstructor mailConstructor;

    @Autowired
    private CartItemService cartItemService;

    @Autowired
    private OrderService orderService;

    @RequestMapping("/")
    public String index() {

        return "watchStore";
    }

    @RequestMapping("/login")
    public String login(Model model) {
        model.addAttribute("classActiveLogin", true);

        return "myAccount";
    }

    @RequestMapping("/admin")
    public String admin() {

        return "admin";
    }

    @RequestMapping("/403")
    public String accessDenied() {

        return "403";
    }

    @RequestMapping("/watchStore")
    public String watchStore(Model model,
                            Principal principal) {
        if (principal != null) {
            String username = principal.getName();
            User user = userService.findByUsername(username);
            model.addAttribute("user", user);
        }

        List<Book> bookList = bookService.findAll();
        model.addAttribute("bookList", bookList);
        model.addAttribute("activeAll", true);

        return "watchStore";
    }

    @RequestMapping("/bookDetail")
    public String bookDetail(
            @PathParam("id") Long id,
            Model model,
            Principal principal) {
        if (principal != null) {
            String username = principal.getName();
            User user = userService.findByUsername(username);
            model.addAttribute("user", user);
        }

        Book book = bookService.findOne(id);
        model.addAttribute("book", book);

        List<Integer> qtyList = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        model.addAttribute("qtyList", qtyList);
        model.addAttribute("qty", 1);

        return "bookDetail";
    }

    @RequestMapping("/forgetPassword")
    public String forgetPassword(HttpServletRequest request,
                                 @ModelAttribute("email") String userEmail,
                                 Model model) {
        model.addAttribute("classActiveForgetPassword", true);

        User user = userService.findByEmail(userEmail);

        if (user == null) {
            model.addAttribute("emailNotExists", true);

            return "myAccount";
        }

        String password = SecurityUtility.randomPassword();
        String encryptedPassword = SecurityUtility.passwordEncoder().encode(password);

        user.setPassword(encryptedPassword);

        userService.save(user);

        String token = UUID.randomUUID().toString();
        userService.createPasswordResetTokenForUser(user, token);

        String appUrl = "http://" + request.getServerName() + ":" + request.getServerPort() + request.getContextPath();

        SimpleMailMessage newEmail = mailConstructor.constructResetTokenEmail(appUrl, request.getLocale(), token, user, password);

        mailSender.send(newEmail);

        model.addAttribute("forgetPasswordEmailSent", true);

        return "myAccount";
    }

    @RequestMapping("/myProfile")
    public String myProfile(Model model, Principal principal) {
        User user = userService.findByUsername(principal.getName());
        model.addAttribute("user", user);
        model.addAttribute("userPaymentList", user.getUserPaymentList());
        model.addAttribute("userShippingList", user.getUserShippingList());
        model.addAttribute("orderList", user.getOrderList());

        UserShipping userShipping = new UserShipping();
        model.addAttribute("userShipping", userShipping);

        List<String> stateList = PLConstants.listOfPLState;
        Collections.sort(stateList);
        model.addAttribute("stateList", stateList);

        model.addAttribute("classActiveEdit", true);
        model.addAttribute("listOfCreditCards", true);
        model.addAttribute("listOfShippingAddresses", true);

        return "myProfile";
    }

    @RequestMapping("/listOfCreditCards")
    public String listOfCreditCards(Model model,
                                    Principal principal,
                                    HttpServletRequest request) {
        User user = userService.findByUsername(principal.getName());
        model.addAttribute("user", user);
        model.addAttribute("userPaymentList", user.getUserPaymentList());
        model.addAttribute("userShippingList", user.getUserShippingList());
        model.addAttribute("orderList", user.getOrderList());

        model.addAttribute("listOfCreditCards", true);
        model.addAttribute("listOfShippingAddresses", true);
        model.addAttribute("classActiveBilling", true);

        return "myProfile";
    }

    @RequestMapping("/addNewCreditCard")
    public String addNewCreditCard(Model model,
                                   Principal principal) {
        User user = userService.findByUsername(principal.getName());
        model.addAttribute("user", user);

        UserBilling userBilling = new UserBilling();
        UserPayment userPayment = new UserPayment();

        model.addAttribute("userBilling", userBilling);
        model.addAttribute("userPayment", userPayment);

        List<String> stateList = PLConstants.listOfPLState;
        Collections.sort(stateList);
        model.addAttribute("stateList", stateList);
        model.addAttribute("userPaymentList", user.getUserPaymentList());
        model.addAttribute("userShippingList", user.getUserShippingList());
        model.addAttribute("orderList", user.getOrderList());

        model.addAttribute("addNewCreditCard", true);
        model.addAttribute("classActiveBilling", true);
        model.addAttribute("listOfShippingAddresses", true);

        return "myProfile";
    }

    @RequestMapping(value = "/addNewCreditCard", method = RequestMethod.POST)
    public String addNewCreditCard(@ModelAttribute("userPayment") UserPayment userPayment,
                                   @ModelAttribute("userBilling") UserBilling userBilling,
                                   Principal principal,
                                   Model model) {
        User user = userService.findByUsername(principal.getName());
        userService.updateUserBilling(userBilling, userPayment, user);

        model.addAttribute("user", user);
        model.addAttribute("userPaymentList", user.getUserPaymentList());
        model.addAttribute("userShippingList", user.getUserShippingList());

        model.addAttribute("listOfCreditCards", true);
        model.addAttribute("classActiveBilling", true);
        model.addAttribute("listOfShippingAddresses", true);
        model.addAttribute("orderList", user.getOrderList());

        return "myProfile";
    }

    @RequestMapping("/updateCreditCard")
    public String updateCreditCard(@ModelAttribute("id") Long creditCardId,
                                   Principal principal,
                                   Model model) {
        User user = userService.findByUsername(principal.getName());
        UserPayment userPayment = userPaymentService.findById(creditCardId);

        if (user.getId() != userPayment.getUser().getId()) {
            return "badRequestPage";
        } else {
            model.addAttribute("user", user);
            UserBilling userBilling = userPayment.getUserBilling();
            model.addAttribute("userPayment", userPayment);
            model.addAttribute("userBilling", userBilling);

            model.addAttribute("addNewCreditCard", true);
            model.addAttribute("classActiveBilling", true);
            model.addAttribute("listOfShippingAddresses", true);

            List<String> stateList = PLConstants.listOfPLState;
            Collections.sort(stateList);
            model.addAttribute("stateList", stateList);
            model.addAttribute("userPaymentList", user.getUserPaymentList());
            model.addAttribute("userShippingList", user.getUserShippingList());
            model.addAttribute("orderList", user.getOrderList());

            return "myProfile";
        }
    }

    @RequestMapping(value = "/setDefaultPayment", method = RequestMethod.POST)
    public String setDefaultPayment(@ModelAttribute("defaultUserPaymentId") Long defaultPaymentId,
                                    Principal principal,
                                    Model model) {
        User user = userService.findByUsername(principal.getName());
        userService.setUserDefaultPayment(defaultPaymentId, user);

        model.addAttribute("user", user);
        model.addAttribute("listOfCreditCards", true);
        model.addAttribute("classActiveBilling", true);
        model.addAttribute("listOfShippingAddresses", true);

        model.addAttribute("userPaymentList", user.getUserPaymentList());
        model.addAttribute("userShippingList", user.getUserShippingList());
        model.addAttribute("orderList", user.getOrderList());

        return "myProfile";
    }

    @RequestMapping("/removeCreditCard")
    public String removeCreditCard(@ModelAttribute("id") Long creditCardId,
                                   Principal principal,
                                   Model model) {
        User user = userService.findByUsername(principal.getName());
        UserPayment userPayment = userPaymentService.findById(creditCardId);

        if (user.getId() != userPayment.getUser().getId()) {
            return "badRequestPage";
        } else {
            userPaymentService.removeById(creditCardId);
            model.addAttribute("user", user);

            model.addAttribute("userPaymentList", user.getUserPaymentList());
            model.addAttribute("userShippingList", user.getUserShippingList());
            model.addAttribute("orderList", user.getOrderList());

            model.addAttribute("listOfCreditCards", true);
            model.addAttribute("classActiveBilling", true);
            model.addAttribute("listOfShippingAddresses", true);


            return "myProfile";
        }
    }

    @RequestMapping("/listOfShippingAddresses")
    public String listOfShippingAddress(Model model,
                                        Principal principal,
                                        HttpServletRequest request) {
        User user = userService.findByUsername(principal.getName());
        model.addAttribute("user", user);

        model.addAttribute("userPaymentList", user.getUserPaymentList());
        model.addAttribute("userShippingList", user.getUserShippingList());
        model.addAttribute("orderList", user.getOrderList());

        model.addAttribute("classActiveShipping", true);
        model.addAttribute("listOfShippingAddresses", true);
        model.addAttribute("listOfCreditCards", true);

        return "myProfile";
    }

    @RequestMapping("/addNewShippingAddress")
    public String addNewShippingAddress(Model model,
                                        Principal principal) {
        User user = userService.findByUsername(principal.getName());
        model.addAttribute("user", user);


        UserShipping userShipping = new UserShipping();

        model.addAttribute("userShipping", userShipping);

        List<String> stateList = PLConstants.listOfPLState;
        Collections.sort(stateList);
        model.addAttribute("stateList", stateList);
        model.addAttribute("userPaymentList", user.getUserPaymentList());
        model.addAttribute("userShippingList", user.getUserShippingList());
        model.addAttribute("orderList", user.getOrderList());

        model.addAttribute("addNewShippingAddress", true);
        model.addAttribute("classActiveShipping", true);
        model.addAttribute("listOfCreditCards", true);

        return "myProfile";
    }

    @RequestMapping(value = "/addNewShippingAddress", method = RequestMethod.POST)
    public String addNewShippingAddress(@ModelAttribute("userShipping") UserShipping userShipping,
                                        Principal principal,
                                        Model model) {
        User user = userService.findByUsername(principal.getName());
        userService.updateUserShipping(userShipping, user);

        model.addAttribute("user", user);
        model.addAttribute("userPaymentList", user.getUserPaymentList());
        model.addAttribute("userShippingList", user.getUserShippingList());
        model.addAttribute("orderList", user.getOrderList());

        model.addAttribute("listOfShippingAddresses", true);
        model.addAttribute("classActiveShipping", true);
        model.addAttribute("listOfCreditCards", true);

        return "myProfile";
    }

    @RequestMapping("/updateUserShipping")
    public String updateUserShipping(@ModelAttribute("id") Long shippingAddressId,
                                     Principal principal,
                                     Model model) {
        User user = userService.findByUsername(principal.getName());
        UserShipping userShipping = userShippingService.findById(shippingAddressId);

        if (user.getId() != userShipping.getUser().getId()) {
            return "badRequestPage";
        } else {
            model.addAttribute("user", user);
            model.addAttribute("userShipping", userShipping);

            List<String> stateList = PLConstants.listOfPLState;
            Collections.sort(stateList);
            model.addAttribute("stateList", stateList);

            model.addAttribute("userPaymentList", user.getUserPaymentList());
            model.addAttribute("userShippingList", user.getUserShippingList());
            model.addAttribute("orderList", user.getOrderList());

            model.addAttribute("addNewShippingAddress", true);
            model.addAttribute("classActiveShipping", true);
            model.addAttribute("listOfCreditCards", true);

            return "myProfile";
        }
    }

    @RequestMapping(value = "/setDefaultShippingAddress", method = RequestMethod.POST)
    public String setDefaultShippingAddress(@ModelAttribute("defaultShippingAddressId") Long defaultShippingId,
                                     Principal principal,
                                     Model model) {
        User user = userService.findByUsername(principal.getName());
        userService.setUserDefaultShipping(defaultShippingId, user);

        model.addAttribute("user", user);

        model.addAttribute("userPaymentList", user.getUserPaymentList());
        model.addAttribute("userShippingList", user.getUserShippingList());
        model.addAttribute("orderList", user.getOrderList());

        model.addAttribute("listOfShippingAddresses", true);
        model.addAttribute("classActiveShipping", true);
        model.addAttribute("listOfCreditCards", true);

        return "myProfile";
    }

    @RequestMapping("/removeUserShipping")
    public String removeUserShipping(@ModelAttribute("id") Long userShippingId,
                                   Principal principal,
                                   Model model) {
        User user = userService.findByUsername(principal.getName());
        UserShipping userShipping = userShippingService.findById(userShippingId);

        if (user.getId() != userShipping.getUser().getId()) {
            return "badRequestPage";
        } else {
            model.addAttribute("user", user);

            userShippingService.removeById(userShippingId);

            model.addAttribute("userPaymentList", user.getUserPaymentList());
            model.addAttribute("userShippingList", user.getUserShippingList());
            model.addAttribute("orderList", user.getOrderList());

            model.addAttribute("listOfShippingAddresses", true);
            model.addAttribute("classActiveShipping", true);
            model.addAttribute("listOfCreditCards", true);

            return "myProfile";
        }
    }

    @RequestMapping(value = "/newUser", method = RequestMethod.POST)
    public String newUserPost(HttpServletRequest request,
                              @ModelAttribute("email") String userEmail,
                              @ModelAttribute("username") String username,
                              Model model) throws Exception {
        model.addAttribute("classActiveNewAccount", true);
        model.addAttribute("email", userEmail);
        model.addAttribute("username", username);

        if (userService.findByUsername(username) != null) {
            model.addAttribute("usernameExists", true);

            return "myAccount";
        }

        if (userService.findByEmail(userEmail) != null) {
            model.addAttribute("emailExists", true);

            return "myAccount";
        }

        User user = new User();
        user.setUsername(username);
        user.setEmail(userEmail);

        String password = SecurityUtility.randomPassword();
        String encryptedPassword = SecurityUtility.passwordEncoder().encode(password);

        user.setPassword(encryptedPassword);

        Role role = new Role();
        role.setRoleId(1);
        role.setName("ROLE_USER");
        Set<UserRole> userRoles = new HashSet<>();
        userRoles.add(new UserRole(user, role));
        userService.createUser(user, userRoles);

        String token = UUID.randomUUID().toString();
        userService.createPasswordResetTokenForUser(user, token);

        String appUrl = "http://" + request.getServerName() + ":" + request.getServerPort() + request.getContextPath();

        SimpleMailMessage email = mailConstructor.constructResetTokenEmail(appUrl, request.getLocale(), token, user, password);

        mailSender.send(email);

        model.addAttribute("emailSent", true);

        return "myAccount";
    }


    @RequestMapping("/newUser")
    public String newAccount(Locale locale,
                             @RequestParam("token") String token,
                             Model model) {
        PasswordResetToken passwordResetToken = userService.getPasswordResetToken(token);

        if (passwordResetToken == null) {
            String message = "Invalid Token!";
            model.addAttribute("message", message);

            return "redirect:/badRequest";
        }

        User user = passwordResetToken.getUser();
        String username = user.getUsername();

        UserDetails userDetails = userSecurityService.loadUserByUsername(username);

        Authentication authentication = new UsernamePasswordAuthenticationToken(userDetails, userDetails.getPassword(),
                userDetails.getAuthorities());

        SecurityContextHolder.getContext().setAuthentication(authentication);

        model.addAttribute("user", user);

        model.addAttribute("classActiveEdit", true);

        return "myProfile";
    }

    @RequestMapping(value = "/updateUserInfo", method = RequestMethod.POST)
    public String updateUserInfo(@ModelAttribute("user") User user,
                                 @ModelAttribute("newPassword") String newPassword,
                                 Model model) throws Exception {
        User currentUser = userService.findById(user.getId());

        if (currentUser == null) {
            throw new Exception("User not found");
        }

        if (userService.findByEmail(user.getEmail()) != null) {
            if (userService.findByEmail(user.getEmail()).getId() != currentUser.getId()) {
                model.addAttribute("emailExists", true);
                return "myProfile";
            }
        }

        if (userService.findByUsername(user.getUsername()) != null) {
            if (userService.findByUsername(user.getUsername()).getId() != currentUser.getId()) {
                model.addAttribute("usernameExists", true);
                return "myProfile";
            }
        }

        if (newPassword != null && !newPassword.isEmpty() && !newPassword.equals((""))) {
            BCryptPasswordEncoder passwordEncoder = SecurityUtility.passwordEncoder();
            String dbPassword = currentUser.getPassword();
            if (passwordEncoder.matches(user.getPassword(), dbPassword)) {
                currentUser.setPassword(passwordEncoder.encode(newPassword));
            } else {
                model.addAttribute("incorrectPassword", true);

                return "myProfile";
            }
        }

        currentUser.setFirstName(user.getFirstName());
        currentUser.setLastName(user.getLastName());
        currentUser.setUsername(user.getUsername());
        currentUser.setEmail(user.getEmail());

        userService.save(currentUser);
        model.addAttribute("user", currentUser);
        model.addAttribute("updateSuccess", true);
        model.addAttribute("classActiveEdit", true);
        model.addAttribute("listOfShippingAddresses", true);
        model.addAttribute("listOfCreditCards", true);

        UserDetails userDetails = userSecurityService.loadUserByUsername(currentUser.getUsername());

        Authentication authentication = new UsernamePasswordAuthenticationToken(userDetails, userDetails.getPassword(),
                userDetails.getAuthorities());

        SecurityContextHolder.getContext().setAuthentication(authentication);

        return "myProfile";
    }

    @RequestMapping("/orderDetail")
    public String orderDetail(@RequestParam("id") Long orderId,
                              Model model,
                              Principal principal) {
        User user = userService.findByUsername(principal.getName());
        Order order = orderService.findOne(orderId);

        if (order.getUser().getId() != user.getId()) {
            return "badRequestPage";
        } else {
            List<CartItem> cartItemList = cartItemService.findByOrder(order);

            model.addAttribute("cartItemList", cartItemList);
            model.addAttribute("user", user);
            model.addAttribute("order", order);

            UserShipping userShipping = new UserShipping();
            model.addAttribute("userShipping", userShipping);

            model.addAttribute("userPaymentList", user.getUserPaymentList());
            model.addAttribute("userShippingList", user.getUserShippingList());
            model.addAttribute("orderList", user.getOrderList());

            List<String> stateList = PLConstants.listOfPLState;
            Collections.sort(stateList);
            model.addAttribute("stateList", stateList);

            model.addAttribute("listOfShippingAddresses", true);
            model.addAttribute("classActiveOrder", true);
            model.addAttribute("listOfCreditCards", true);
            model.addAttribute("displayOrderDetail", true);

            return "myProfile";
        }

    }

}
