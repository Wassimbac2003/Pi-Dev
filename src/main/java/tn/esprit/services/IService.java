package tn.esprit.services;

public interface IService<T> {
    public void ajouter(T t);
    public void supprimer(T t);
    public void modifier(T t);
    public void getAll();
    public void getOneById(int id);
}
