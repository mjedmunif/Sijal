package org.example.sijalsystem.Service;

import lombok.RequiredArgsConstructor;
import org.example.sijalsystem.API.APIException;
import org.example.sijalsystem.DTO.OUT.PaymentResult;
import org.example.sijalsystem.Model.Card;
import org.example.sijalsystem.Model.Customer;
import org.example.sijalsystem.Model.Subscription;
import org.example.sijalsystem.Repository.CustomerRepository;
import org.example.sijalsystem.Repository.SubscriptionRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final CustomerRepository customerRepository;
    private final PaymentService paymentService;

    public List<Subscription> getSubscriptions(){
        return subscriptionRepository.findAll();
    }

    public PaymentResult subscribe(Integer customer_id) {

        Customer customer = customerRepository.findCustomerById(customer_id);
        if (customer == null) {
            throw new APIException("Customer not found");
        }

        if (customer.getCardSet().isEmpty()) {
            throw new APIException("Card not found, please enter your card first");
        }


        boolean hasActiveSubscription = customer.getSubscriptionSet().stream()
                .anyMatch(s -> s.getEndDate().isAfter(LocalDate.now()));

        if (hasActiveSubscription) {
            throw new APIException("You already have an active subscription");
        }


        return createSubscriptionAndPay(customer);
    }

    private PaymentResult createSubscriptionAndPay(Customer customer) {

        Card card = customer.getCardSet().stream()
                .findFirst()
                .orElseThrow(() -> new APIException("No card found"));

        Subscription newSubscription = new Subscription();
        newSubscription.setCustomer(customer);
        newSubscription.setStartDate(LocalDate.now());
        newSubscription.setEndDate(LocalDate.now().plusMonths(1));
        subscriptionRepository.save(newSubscription);

        return paymentService.processPayment(card, 50);
    }



    public void deleteSubscription(Integer customer_id,Integer subscription_id){
        Customer customer = customerRepository.findCustomerById(customer_id);
        Subscription subscription = subscriptionRepository.findSubscriptionById(subscription_id);
        if(customer == null || subscription == null){
            throw new APIException("Customer or subscription not found");
        }
        if(!subscription.getCustomer().getId().equals(customer_id)){
            throw new APIException("Customer not authorized to delete subscription");
        }
        subscriptionRepository.delete(subscription);
    }
}
