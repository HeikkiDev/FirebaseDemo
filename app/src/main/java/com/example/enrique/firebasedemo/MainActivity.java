package com.example.enrique.firebasedemo;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

import com.firebase.ui.database.FirebaseListAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class MainActivity extends AppCompatActivity {

    private ListView listView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        listView = (ListView)findViewById(R.id.listView);

        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference().child("products");

        FirebaseListAdapter<Product> firebaseListAdapter = new FirebaseListAdapter<Product>(
                this,
                Product.class,
                android.R.layout.two_line_list_item, //android.R.layout.simple_list_item_1,
                databaseReference
        ) {
            @Override
            protected void populateView(final View v, Product model, int position) {
                TextView textView = (TextView) v.findViewById(android.R.id.text1);
                final TextView textView2 = (TextView) v.findViewById(android.R.id.text2);

                textView.setText(model.Name.toString());

                String cateogry = "";
                FirebaseDatabase.getInstance().getReference().child("categories").child(model.Category.toString()).child("Name").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        String cateogry = dataSnapshot.getValue(String.class);
                        textView2.setText("Categoría: " + cateogry);
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        //
                    }
                });

            }
        };

        listView.setAdapter(firebaseListAdapter);
    }

    private void SignOut(){
        FirebaseAuth.getInstance().signOut();
    }

    @Override
    public void onBackPressed()
    {
        SignOut();
        super.onBackPressed();
    }
}
